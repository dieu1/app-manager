const {onDocumentCreated, onDocumentWritten} = require("firebase-functions/v2/firestore");
const {initializeApp} = require("firebase-admin/app");
const {getFirestore} = require("firebase-admin/firestore");
const {getMessaging} = require("firebase-admin/messaging");

initializeApp();

async function tokensFor(userIds) {
  const unique = [...new Set(userIds.filter(Boolean))];
  const snapshots = await Promise.all(unique.map((userId) =>
    getFirestore().collection("user_devices").where("userId", "==", userId).get()));
  return [...new Set(snapshots.flatMap((snapshot) =>
    snapshot.docs.map((document) => document.get("token")).filter(Boolean)))];
}

async function notify(userIds, title, body, data = {}) {
  const tokens = await tokensFor(userIds);
  if (!tokens.length) return;
  await getMessaging().sendEachForMulticast({
    tokens,
    notification: {title, body},
    data: Object.fromEntries(Object.entries(data).map(([key, value]) =>
      [key, value == null ? "" : String(value)])),
    android: {priority: "high"}
  });
}

function assigneesFor(task) {
  if (!task) return [];
  if (Array.isArray(task.assigneeIds)) return task.assigneeIds.filter(Boolean);
  return task.assigneeId ? [task.assigneeId] : [];
}

exports.onTeamInviteCreated = onDocumentCreated("team_invites/{inviteId}", async (event) => {
  const invite = event.data.data();
  if (invite.status !== "PENDING") return;
  await notify([invite.invitedUserId], "Lời mời tham gia Team",
    `Bạn được mời tham gia ${invite.workspaceName || "một Team"}`,
    {type: "TEAM_INVITE", workspaceId: invite.workspaceId});
});

exports.onTeamTaskWritten = onDocumentWritten("tasks/{taskId}", async (event) => {
  const before = event.data.before.exists ? event.data.before.data() : null;
  const after = event.data.after.exists ? event.data.after.data() : null;
  if (!after || !after.projectId) return;
  const beforeAssignees = new Set(assigneesFor(before));
  const afterAssignees = assigneesFor(after);
  const newlyAssigned = afterAssignees.filter((id) => !beforeAssignees.has(id));
  if (!before || newlyAssigned.length) {
    await notify(newlyAssigned.length ? newlyAssigned : afterAssignees,
      "Công việc mới được giao", after.title,
      {type: "TASK_ASSIGNED", workspaceId: after.workspaceId, taskId: after.taskId});
  } else if (before.dueDate !== after.dueDate || before.status !== after.status) {
    await notify(afterAssignees, "Công việc vừa được cập nhật", after.title,
      {type: "TASK_UPDATED", workspaceId: after.workspaceId, taskId: after.taskId});
  }
});

exports.onTaskCommentCreated = onDocumentCreated("task_comments/{commentId}", async (event) => {
  const comment = event.data.data();
  if (comment.deletedAt) return;
  const taskSnapshot = await getFirestore().collection("tasks").doc(comment.taskId).get();
  if (!taskSnapshot.exists) return;
  const task = taskSnapshot.data();
  await notify([...assigneesFor(task), task.createdBy].filter((id) => id !== comment.userId),
    "Bình luận mới", task.title,
    {type: "TASK_COMMENT", workspaceId: task.workspaceId, taskId: task.taskId});
});

exports.onSubtaskCompleted = onDocumentWritten("task_subtasks/{subtaskId}", async (event) => {
  const before = event.data.before.exists ? event.data.before.data() : null;
  const after = event.data.after.exists ? event.data.after.data() : null;
  if (!after || !after.completed || (before && before.completed)) return;
  const taskSnapshot = await getFirestore().collection("tasks").doc(after.taskId).get();
  if (!taskSnapshot.exists) return;
  const task = taskSnapshot.data();
  await notify([...assigneesFor(task), task.createdBy], "Đã hoàn thành một bước",
    `${after.title} · ${task.title}`,
    {type: "SUBTASK_COMPLETED", workspaceId: task.workspaceId, taskId: task.taskId});
});
