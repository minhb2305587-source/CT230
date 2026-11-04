package vn.edu.bookstore.catalog.api;

import java.net.URI;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import vn.edu.bookstore.catalog.api.generated.BooksApi;
import vn.edu.bookstore.catalog.api.model.Book;
import vn.edu.bookstore.catalog.api.model.BookCreateRequest;
import vn.edu.bookstore.catalog.api.model.BookPage;
import vn.edu.bookstore.catalog.api.model.BookUpdateRequest;
import vn.edu.bookstore.catalog.domain.BookEntity;
import vn.edu.bookstore.catalog.service.BookService;

@RestController
public class BookController implements BooksApi {

    private final BookService service;

    public BookController(BookService service) {
        this.service = service;
    }

    // TH2-4: Chuyển tham số sort thành Spring Data Sort (chuẩn hoá thuộc tính & khoá phụ isbn)
    static Sort toSort(String sort) {
        String[] parts = sort.split(",");
        String property = switch (parts[0]) {
            case "price" -> "priceAmount";
            case "publishedYear" -> "publishedYear";
            default -> "title";
        };
        Sort.Direction dir = parts.length > 1 && parts[1].equals("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(dir, property).and(Sort.by("isbn"));
    }

    // TH2-4: Phân trang + sắp xếp theo tham số đã khai báo trong hợp đồng
    @Override
    public ResponseEntity<BookPage> listBooks(String q, Integer page, Integer size, String sort) {
        return ResponseEntity.ok(BookMapper.toPage(service.list(q, PageRequest.of(page, size, toSort(sort)))));
    }

    @Override
    public ResponseEntity<Book> getBook(String isbn, String ifNoneMatch) {
        BookEntity e = service.get(isbn);
        String etag = BookMapper.etag(e);

        if (ifNoneMatch != null && ifNoneMatch.equals(etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
        }

        return ResponseEntity.ok().eTag(etag).body(BookMapper.toApi(e));
    }

    @Override
    public ResponseEntity<Book> createBook(BookCreateRequest request) {
        BookEntity e = service.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{isbn}").buildAndExpand(e.getIsbn()).toUri();
        return ResponseEntity.created(location).eTag(BookMapper.etag(e)).body(BookMapper.toApi(e));
    }

    @Override
    public ResponseEntity<Book> updateBook(String isbn, BookUpdateRequest request, String ifMatch) {
        if (ifMatch == null || ifMatch.isBlank()) {
            throw new IllegalArgumentException("Thiếu header If-Match (yêu cầu mã 428)");
        }

        BookEntity current = service.get(isbn);
        String currentEtag = BookMapper.etag(current);
        if (!ifMatch.equals(currentEtag)) {
            throw new IllegalStateException("ETag không khớp (yêu cầu mã 412)");
        }

        BookEntity updated = service.update(isbn, request);
        return ResponseEntity.ok().eTag(BookMapper.etag(updated)).body(BookMapper.toApi(updated));
    }

    @Override
    public ResponseEntity<Void> deleteBook(String isbn) {
        service.delete(isbn);
        return ResponseEntity.noContent().build();
    }
}
