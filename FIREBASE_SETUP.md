# Kết nối Firebase cho Task Manager

Ứng dụng dùng Firebase Authentication để quản lý tài khoản và Cloud Firestore để đồng bộ dữ liệu giữa nhiều thiết bị. Bản hiện tại chủ động chạy ở chế độ Spark miễn phí: ảnh đại diện dùng chữ cái tên người dùng, còn Storage và Cloud Functions được tắt. Mỗi tài khoản được cấp một mã công khai duy nhất dạng `USR-XXXXXXXXXXXX`; người dùng chia sẻ mã này để tham gia nhóm, không cần công khai Firebase UID.

## Trạng thái môi trường staging (23/08/2026)

- Project: `apptaskmanager-9f5e4` (alias `staging`).
- Android app/package: đã kết nối đúng `com.vandieu_manhdung.taskmanager`.
- Firestore `(default)`: đã tạo tại `nam5`; `firestore.rules` đã biên dịch và triển khai thành công.
- Firebase Storage: không dùng trong bản Spark; nút tải avatar/tệp Team đã được ẩn.
- Cloud Functions: API chưa được bật và chưa triển khai; thông báo FCM phát sinh từ thay đổi của thiết bị khác chưa hoạt động.
- Thông báo lịch công việc cục bộ trên chính thiết bị vẫn hoạt động mà không cần Cloud Functions.

Không cần bật thanh toán để tiếp tục kiểm thử các chức năng chính. Chỉ khi sau này cần tải tệp cloud hoặc thông báo đẩy từ thiết bị khác mới cân nhắc Storage/Functions và gói Blaze.

## 1. Tạo Firebase project và Android app

1. Mở [Firebase Console](https://console.firebase.google.com/) và chọn **Create a project**.
2. Trong Project overview, chọn **Add app** > **Android**.
3. Nhập Android package name chính xác: `com.vandieu_manhdung.taskmanager`.
4. Tải tệp `google-services.json`.
5. Chép tệp vào đúng vị trí: `E:\taskmanager\app\google-services.json`.
6. Build lại ứng dụng. Plugin Google Services của dự án sẽ tự đọc tệp này.

Không đổi package name sau khi tải tệp. Email/Password chưa cần SHA-1; SHA-1 sẽ cần cho một số nhà cung cấp như Google Sign-In nếu tích hợp sau.

Tài liệu chính thức: https://firebase.google.com/docs/android/setup

## 2. Bật đăng nhập Email/Password

1. Firebase Console > **Build** > **Authentication**.
2. Chọn **Get started** nếu đây là lần đầu.
3. Mở tab **Sign-in method**.
4. Chọn **Email/Password**, bật mục đầu tiên rồi nhấn **Save**.
5. Vào **Templates** để chỉnh tên ứng dụng, email xác minh và email đặt lại mật khẩu.

Ứng dụng bắt buộc người dùng xác minh email trước khi đăng nhập. Tài liệu chính thức: https://firebase.google.com/docs/auth/android/password-auth

## 3. Tạo Cloud Firestore

1. Firebase Console > **Build** > **Firestore Database**.
2. Chọn **Create database**.
3. Chọn **Production mode**.
4. Chọn region gần phần lớn người dùng và giữ nguyên `(default)` database.

Không cần tự tạo collection. Ứng dụng tự tạo các collection tài khoản, Team, lời mời, dự án, công việc, checklist, lịch sử, bình luận, tệp, phụ thuộc và thiết bị nhận FCM.

## 4. Firebase Storage (tùy chọn sau này)

Không thực hiện mục này trong chế độ Spark hiện tại. Nếu sau này chuyển sang gói phù hợp:

1. Firebase Console > **Build** > **Storage**.
2. Chọn **Get started**, chọn cùng region với Firestore nếu có thể.
3. Không dùng rules thử nghiệm; `storage.rules` giới hạn avatar và tệp Team theo thành viên đang hoạt động (tệp Team tối đa 20 MB).

## 5. Triển khai luật bảo mật

Dự án đã có sẵn `firebase.json`, `firestore.rules` và `storage.rules`. Trên Windows, cài Firebase CLI bằng tệp standalone hoặc Node.js/npm:

```powershell
npm install -g firebase-tools
```

Sau đó chạy trong PowerShell:

```powershell
cd E:\taskmanager
firebase login
firebase use staging
firebase deploy --only firestore:rules --project staging
```

Dự án hiện đã ánh xạ alias `staging` tới Firebase project `apptaskmanager-9f5e4`. Nếu bạn muốn dùng một project khác, chạy `firebase use --add`, chọn project đó rồi đặt alias phù hợp. Việc deploy bằng CLI sẽ thay thế rules đang có trên Firebase Console bằng tệp `firestore.rules` trong dự án.

Tài liệu chính thức: https://firebase.google.com/docs/cli và https://firebase.google.com/docs/firestore/security/get-started

## 6. Build và chạy

Trong Android Studio, chọn **Sync Project with Gradle Files**, sau đó Run. Hoặc dùng PowerShell:

```powershell
cd E:\taskmanager
.\gradlew.bat assembleDebug
```

APK debug nằm tại `app\build\outputs\apk\debug\app-debug.apk`.

Nếu ứng dụng vẫn hiện màn hình “Cần kết nối Firebase”, kiểm tra:

- tên tệp phải đúng là `google-services.json`, không phải `google-services (1).json`;
- tệp nằm trong thư mục `app`, không nằm ở thư mục gốc;
- `package_name` bên trong tệp là `com.vandieu_manhdung.taskmanager`;
- đã build lại sau khi chép tệp.

## 7. Kiểm thử đúng theo mô hình nhiều thiết bị

1. Cài ứng dụng trên hai thiết bị hoặc hai emulator khác nhau.
2. Đăng ký tài khoản A và B, sau đó xác minh cả hai email.
3. Đăng nhập A, mở **Tài khoản** hoặc **Trang chủ**, sao chép mã `USR-...`.
4. Đăng nhập B trên thiết bị thứ hai và sao chép mã của B.
5. Trên thiết bị A, tạo nhóm > **Thêm thành viên** > nhập mã của B.
6. Thiết bị B mở **Lời mời tham gia**, chọn lời mời và nhấn **Đồng ý**; chỉ sau đó Team mới xuất hiện.
7. Kiểm tra từ chối lời mời, lời mời hết hạn, rời Team và chuyển quyền chủ nhóm.
8. Tạo dự án có ngày giờ, mốc dự án, task có checklist, nhiều người thực hiện, bình luận và phụ thuộc; thiết bị còn lại phải cập nhật tự động.
9. Giao task cho B và kiểm tra thông báo lịch cục bộ khi ứng dụng ở nền.
10. Tắt mạng, sửa task/dự án, bật lại mạng và kiểm tra hàng đợi đồng bộ được đẩy lên Firestore.

Cloud Functions cần Firebase project ở gói Blaze để triển khai trong môi trường production. Khi chưa triển khai Functions, nhắc giờ bắt đầu/sắp đến hạn trên chính thiết bị vẫn hoạt động; thông báo đẩy từ thiết bị khác chưa hoạt động.

## 8. Trước khi phát hành thật

- Dùng Firebase project riêng cho development, staging và production.
- Bật App Check và cấu hình Play Integrity.
- Thiết lập cảnh báo ngân sách trong Google Cloud Billing.
- Chạy Firebase Emulator để kiểm thử rules trước mỗi lần deploy.
- Không đưa service-account key hoặc khóa quản trị máy chủ vào ứng dụng Android.
