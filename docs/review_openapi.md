# Review hợp đồng OpenAPI 3.1 - Catalog Service (TH2-1)

## 1. Đánh giá Mã trạng thái HTTP (RFC 9110 & RFC 9457)
- **GET /api/v1/books**: `200 OK` cho danh sách phân trang, `400 Bad Request` nếu tham số không hợp lệ.
- **POST /api/v1/books**: `201 Created` kèm header `Location` (trỏ tới tài nguyên mới) và `ETag`. Trả về `400` nếu dữ liệu lỗi validation, `409 Conflict` nếu ISBN đã tồn tại.
- **GET /api/v1/books/{isbn}`**: `200 OK` kèm `ETag`. Nếu client gửi `If-None-Match` khớp với ETag hiện tại, trả về `304 Not Modified` (không trả về body). Trả về `404 Not Found` nếu không tìm thấy.
- **PUT /api/v1/books/{isbn}`**: `200 OK` kèm `ETag` mới. Nếu client thiếu header `If-Match` trả về `428 Precondition Required`. Nếu `If-Match` không khớp ETag server trả về `412 Precondition Failed`. Trả về `400` (lỗi validation) hoặc `404` (không tìm thấy).
- **DELETE /api/v1/books/{isbn}`**: `204 No Content` khi xóa thành công (không trả về body), `404 Not Found` nếu tài nguyên không tồn tại.
=> Tất cả các mã trạng thái và cấu trúc lỗi (`application/problem+json`) đã tuân thủ chuẩn RFC 9110 và RFC 9457.

## 2. Đối chiếu Ràng buộc Isbn13 và Money với XSD ở TH1
- **Isbn13**: RegEx `^97[89][0-9]{10}$` đảm bảo mã ISBN gồm 13 chữ số bắt đầu bằng 978 hoặc 979, đúng với ràng buộc nghiệp vụ XSD ở TH1.
- **Money**: `minimum: 0` (giá không âm), `multipleOf: 0.01` (làm tròn đến 2 chữ số thập phân), hỗ trợ loại tiền `VND` và `USD`. Hoàn toàn tương thích với định dạng XSD TH1.

## 3. Tính An toàn (Safe), Idempotent và Vai trò của If-Match
Bảng tổng hợp thuộc tính HTTP:

| Thao tác | An toàn (Safe)? | Idempotent? | Mã thành công | Mã lỗi đã khai báo |
| :--- | :--- | :--- | :--- | :--- |
| `GET /api/v1/books` | Có | Có | 200 | 400 |
| `POST /api/v1/books` | Không | Không | 201 + Location + ETag | 400, 409 |
| `GET /api/v1/books/{isbn}` | Có | Có | 200 + ETag / 304 | 404 |
| `PUT /api/v1/books/{isbn}` | Không | Có | 200 + ETag mới | 400, 404, 412, 428 |
| `DELETE /api/v1/books/{isbn}` | Không | Có | 204 | 404 |

- **Vì sao PUT cần header `If-Match`**: Để thực hiện cơ chế **Khóa lạc quan (Optimistic Locking)** nhằm giải quyết bài toán ghi đè mất dữ liệu (Lost Update Problem). Server kiểm tra ETag gửi lên trong `If-Match`. Nếu một giao dịch khác đã cập nhật bản ghi trước đó, ETag sẽ thay đổi và server chặn yêu cầu cập nhật bằng mã `412 Precondition Failed`.

## 4. Đánh giá tính Tương thích ngược (Backward Compatibility)
- **Nếu thêm trường bắt buộc mới (`required`) vào `BookCreateRequest`**:
  - **Có phá vỡ tương thích ngược**.
  - **Giải thích**: Các ứng dụng phía client cũ chưa cập nhật mã nguồn sẽ gửi request thiếu trường bắt buộc mới, dẫn đến việc request bị server từ chối với lỗi `400 Bad Request` (Validation Failed). Để đảm bảo tương thích ngược, trường mới nên là không bắt buộc (optional) hoặc bổ sung giá trị mặc định (`default`).
