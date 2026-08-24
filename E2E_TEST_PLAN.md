# Kịch bản kiểm thử hai tài khoản, nhiều thiết bị

Kịch bản này kiểm tra luồng thật với Firebase staging, không dùng chung dữ liệu cục bộ của một thiết bị. Phần Authentication, Firestore, lời mời bằng mã `USR-...`, đồng bộ Team và nhắc việc cục bộ đều chạy được trên gói Spark.

## Chuẩn bị

- Hai thiết bị Android hoặc hai emulator có thư mục dữ liệu độc lập.
- Tài khoản A và B dùng hai email thật khác nhau, đã bấm liên kết xác minh email.
- Cả hai bản cài dùng cùng `app/google-services.json` của project staging.

## Luồng bắt buộc

1. A tạo Team và dự án có giờ bắt đầu, hạn hoàn thành.
2. B sao chép mã cá nhân `USR-...`; A mời B bằng mã đó.
3. B chấp nhận lời mời. Cả hai thiết bị phải thấy cùng Team mà không cần đăng xuất.
4. A tạo công việc và chọn đồng thời A, B làm người thực hiện.
5. B sửa trạng thái hoặc checklist. Tiến độ phải tự tính lại và xuất hiện trên thiết bị A.
6. A và B lần lượt thêm, sửa, xóa bình luận theo đúng quyền.
7. Tạo ba công việc A → B → C, sau đó thử thêm C → A. Ứng dụng phải chặn vòng lặp phụ thuộc.
8. Mở Dashboard, kiểm tra biểu đồ Gantt có trục ngày và thanh thời gian đúng ngày bắt đầu/hạn.
9. Tạo hơn 20 công việc, kiểm tra nút **Tải thêm công việc** và các bộ lọc.
10. Tắt mạng ở thiết bị B, cập nhật checklist/bình luận, bật mạng và xác nhận dữ liệu đồng bộ lại.

## Thông báo

- Trên mỗi thiết bị được giao việc, xác nhận thông báo lúc bắt đầu, trước hạn một giờ và khi quá hạn.
- Thông báo đẩy do thay đổi từ thiết bị khác cần Cloud Functions/FCM và có thể yêu cầu gói Blaze. Khi chỉ dùng Spark, bỏ qua bước này; ứng dụng vẫn lưu thông báo trong app và nhắc giờ cục bộ.
- Tệp đính kèm và ảnh đại diện cloud đang tắt trong bản Spark; avatar dùng chữ cái tên người dùng.

## Tiêu chí đạt

- Không tài khoản ngoài Team đọc hoặc sửa được dữ liệu Team.
- Không mất dữ liệu khi chuyển ngoại tuyến/trực tuyến.
- Không tạo task phụ thuộc khác dự án hoặc tạo vòng lặp.
- Mọi thành viên được giao đều có quyền cập nhật task; thành viên không được giao chỉ có quyền theo vai trò.
- Cả hai thiết bị hiển thị cùng số liệu Dashboard sau khi đồng bộ.
