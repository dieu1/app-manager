# Kết nối Firebase cho Task Manager

Ứng dụng dùng Firebase Authentication để quản lý tài khoản và Cloud Firestore để đồng bộ dữ liệu giữa nhiều thiết bị. Mỗi tài khoản được cấp một mã công khai duy nhất dạng `USR-XXXXXXXXXXXX`; người dùng chia sẻ mã này để tham gia nhóm, không cần công khai Firebase UID.

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

Không cần tự tạo collection. Sau khi người dùng đăng ký và sử dụng ứng dụng, các collection `users`, `user_codes`, `workspaces`, `workspace_members`, `projects`, `tasks` và `work_sessions` sẽ được tạo tự động.

## 4. Triển khai luật bảo mật

Dự án đã có sẵn `firebase.json` và `firestore.rules`. Trên Windows, cài Firebase CLI bằng tệp standalone hoặc Node.js/npm:

```powershell
npm install -g firebase-tools
```

Sau đó chạy trong PowerShell:

```powershell
cd E:\taskmanager
firebase login
firebase use --add
firebase deploy --only firestore
```

Khi `firebase use --add` hỏi project, chọn project vừa tạo và đặt alias là `default`. Việc deploy bằng CLI sẽ thay thế rules đang có trên Firebase Console bằng tệp `firestore.rules` trong dự án.

Tài liệu chính thức: https://firebase.google.com/docs/cli và https://firebase.google.com/docs/firestore/security/get-started

## 5. Build và chạy

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

## 6. Kiểm thử đúng theo mô hình nhiều thiết bị

1. Cài ứng dụng trên hai thiết bị hoặc hai emulator khác nhau.
2. Đăng ký tài khoản A và B, sau đó xác minh cả hai email.
3. Đăng nhập A, mở **Tài khoản** hoặc **Trang chủ**, sao chép mã `USR-...`.
4. Đăng nhập B trên thiết bị thứ hai và sao chép mã của B.
5. Trên thiết bị A, tạo nhóm > **Thêm thành viên** > nhập mã của B.
6. Nhóm phải tự xuất hiện trên thiết bị B.
7. Tạo và sửa task ở một thiết bị; thiết bị còn lại phải cập nhật tự động.
8. Tắt mạng, tạo task, bật lại mạng và kiểm tra task được đẩy lên Firestore.

## 7. Trước khi phát hành thật

- Dùng Firebase project riêng cho development, staging và production.
- Bật App Check và cấu hình Play Integrity.
- Thiết lập cảnh báo ngân sách trong Google Cloud Billing.
- Chạy Firebase Emulator để kiểm thử rules trước mỗi lần deploy.
- Không đưa service-account key hoặc khóa quản trị máy chủ vào ứng dụng Android.
