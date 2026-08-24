const fs = require("node:fs");
const path = require("node:path");
const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds
} = require("@firebase/rules-unit-testing");
const {
  doc,
  getDoc,
  setDoc,
  updateDoc,
  deleteDoc
} = require("firebase/firestore");

const PROJECT_ID = "demo-taskmanager-rules";
const ROOT = path.resolve(__dirname, "..");

let testEnv;

function dbFor(userId) {
  return testEnv.authenticatedContext(userId, {
    email: `${userId}@example.com`
  }).firestore();
}

async function seedWorkspace() {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    const now = Date.now();
    const members = [
      ["ws-a_owner", "ws-a", "owner", "OWNER"],
      ["ws-a_member", "ws-a", "member", "MEMBER"],
      ["ws-a_member2", "ws-a", "member2", "MEMBER"],
      ["ws-a_manager", "ws-a", "manager", "ADMIN"],
      ["ws-b_outsider", "ws-b", "outsider", "OWNER"]
    ];
    for (const [id, workspaceId, userId, role] of members) {
      await setDoc(doc(db, "workspace_members", id), {
        workspaceId,
        userId,
        userCode: `USR-${userId.toUpperCase()}`,
        role,
        status: "ACTIVE",
        joinedAt: now
      });
    }
    await setDoc(doc(db, "workspaces", "ws-a"), {
      workspaceId: "ws-a",
      managerId: "owner",
      name: "Team A"
    });
    await setDoc(doc(db, "workspaces", "ws-b"), {
      workspaceId: "ws-b",
      managerId: "outsider",
      name: "Team B"
    });
    await setDoc(doc(db, "tasks", "task-a1"), {
      taskId: "task-a1",
      workspaceId: "ws-a",
      projectId: "project-a",
      createdBy: "owner",
      assigneeId: "member",
      assigneeIds: ["member", "member2"],
      title: "Task A1"
    });
    await setDoc(doc(db, "tasks", "task-a2"), {
      taskId: "task-a2",
      workspaceId: "ws-a",
      projectId: "project-a",
      createdBy: "owner",
      assigneeId: "member",
      assigneeIds: ["member"],
      title: "Task A2"
    });
    await setDoc(doc(db, "tasks", "task-a-other-project"), {
      taskId: "task-a-other-project",
      workspaceId: "ws-a",
      projectId: "project-other",
      createdBy: "owner",
      assigneeId: "member",
      assigneeIds: ["member"],
      title: "Other project"
    });
    await setDoc(doc(db, "tasks", "task-b1"), {
      taskId: "task-b1",
      workspaceId: "ws-b",
      projectId: "project-b",
      createdBy: "outsider",
      assigneeId: "outsider",
      assigneeIds: ["outsider"],
      title: "Task B1"
    });
  });
}

function commentData(overrides = {}) {
  const now = Date.now();
  return {
    commentId: "comment-1",
    taskId: "task-a1",
    workspaceId: "ws-a",
    userId: "member",
    message: "Cập nhật tiến độ",
    createdAt: now,
    updatedAt: now,
    deletedAt: 0,
    ...overrides
  };
}

describe("Task Manager Firestore rules", () => {
  before(async () => {
    testEnv = await initializeTestEnvironment({
      projectId: PROJECT_ID,
      firestore: {
        rules: fs.readFileSync(path.join(ROOT, "firestore.rules"), "utf8")
      }
    });
  });

  beforeEach(async () => {
    await testEnv.clearFirestore();
    await seedWorkspace();
  });

  after(async () => {
    await testEnv.cleanup();
  });

  it("chỉ thành viên active mới đọc được task trong workspace", async () => {
    await assertSucceeds(getDoc(doc(dbFor("member"), "tasks", "task-a1")));
    await assertFails(getDoc(doc(dbFor("outsider"), "tasks", "task-a1")));
  });

  it("mọi thành viên được giao đều có thể cập nhật task", async () => {
    await assertSucceeds(updateDoc(
      doc(dbFor("member2"), "tasks", "task-a1"),
      {status: "IN_PROGRESS", updatedAt: Date.now()}
    ));
    await assertFails(updateDoc(
      doc(dbFor("outsider"), "tasks", "task-a1"),
      {status: "COMPLETED", updatedAt: Date.now()}
    ));
  });

  it("cho phép bình luận đúng task/workspace và chặn tham chiếu chéo", async () => {
    await assertSucceeds(setDoc(
      doc(dbFor("member"), "task_comments", "comment-1"),
      commentData()
    ));
    await assertFails(setDoc(
      doc(dbFor("member"), "task_comments", "comment-cross"),
      commentData({commentId: "comment-cross", taskId: "task-b1"})
    ));
  });

  it("chỉ tác giả được sửa nội dung, Admin được xóa mềm", async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      await setDoc(doc(context.firestore(), "task_comments", "comment-1"), commentData());
    });
    await assertSucceeds(updateDoc(
      doc(dbFor("member"), "task_comments", "comment-1"),
      {message: "Nội dung đã sửa", updatedAt: Date.now()}
    ));
    await assertFails(updateDoc(
      doc(dbFor("outsider"), "task_comments", "comment-1"),
      {message: "Không được phép", updatedAt: Date.now()}
    ));
    await assertSucceeds(updateDoc(
      doc(dbFor("manager"), "task_comments", "comment-1"),
      {deletedAt: Date.now(), updatedAt: Date.now()}
    ));
  });

  it("kiểm tra task/workspace và kích thước metadata của attachment", async () => {
    const base = {
      attachmentId: "attachment-1",
      taskId: "task-a1",
      workspaceId: "ws-a",
      userId: "member",
      displayName: "result.txt",
      mimeType: "text/plain",
      remoteUrl: "https://example.invalid/result.txt",
      sizeBytes: 120,
      createdAt: Date.now(),
      deletedAt: 0
    };
    await assertSucceeds(setDoc(
      doc(dbFor("member"), "task_attachments", "attachment-1"), base
    ));
    await assertFails(setDoc(
      doc(dbFor("member"), "task_attachments", "attachment-cross"),
      {...base, attachmentId: "attachment-cross", taskId: "task-b1"}
    ));
    await assertFails(setDoc(
      doc(dbFor("member"), "task_attachments", "attachment-large"),
      {...base, attachmentId: "attachment-large", sizeBytes: 21 * 1024 * 1024}
    ));
  });

  it("chỉ cho phép dependency cùng workspace và cùng project", async () => {
    const base = {
      taskId: "task-a2",
      dependsOnTaskId: "task-a1",
      workspaceId: "ws-a",
      createdBy: "member",
      createdAt: Date.now()
    };
    await assertSucceeds(setDoc(
      doc(dbFor("member"), "task_dependencies", "task-a2_task-a1"), base
    ));
    await assertFails(setDoc(
      doc(dbFor("member"), "task_dependencies", "task-a2_task-a-other-project"),
      {...base, dependsOnTaskId: "task-a-other-project"}
    ));
    await assertFails(setDoc(
      doc(dbFor("member"), "task_dependencies", "task-a2_task-b1"),
      {...base, dependsOnTaskId: "task-b1"}
    ));
    await assertSucceeds(deleteDoc(
      doc(dbFor("member"), "task_dependencies", "task-a2_task-a1")
    ));
  });
});
