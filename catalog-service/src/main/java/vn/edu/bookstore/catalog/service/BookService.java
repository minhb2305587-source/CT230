package vn.edu.bookstore.catalog.service;

import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.bookstore.catalog.api.model.BookCreateRequest;
import vn.edu.bookstore.catalog.api.model.BookUpdateRequest;
import vn.edu.bookstore.catalog.domain.BookEntity;
import vn.edu.bookstore.catalog.domain.BookRepository;

@Service
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository repository;

    public BookService(BookRepository repository) {
        this.repository = repository;
    }

    public Page<BookEntity> list(String q, Pageable pageable) {
        if (q == null || q.isBlank()) {
            return repository.findAll(pageable);
        }
        return repository.findByTitleContainingIgnoreCaseOrAuthorContainingIgnoreCase(q.trim(), q.trim(), pageable);
    }

    public BookEntity get(String isbn) {
        return repository.findByIsbn(isbn)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sách ISBN: " + isbn));
    }

    @Transactional
    public BookEntity create(BookCreateRequest request) {
        if (repository.existsByIsbn(request.getIsbn())) {
            throw new IllegalArgumentException("ISBN đã tồn tại: " + request.getIsbn());
        }
        BigDecimal priceAmount = request.getPrice() != null ? request.getPrice().getAmount() : BigDecimal.ZERO;
        String priceCurrency = (request.getPrice() != null && request.getPrice().getCurrency() != null) 
                ? request.getPrice().getCurrency().getValue() : "VND";

        BookEntity entity = new BookEntity(
                request.getIsbn(),
                request.getTitle(),
                request.getAuthor(),
                request.getAuthorId(),
                request.getPublishedYear(),
                priceAmount,
                priceCurrency
        );
        return repository.save(entity);
    }

    @Transactional
    public BookEntity update(String isbn, BookUpdateRequest request) {
        BookEntity entity = get(isbn);
        BigDecimal priceAmount = request.getPrice() != null ? request.getPrice().getAmount() : BigDecimal.ZERO;
        String priceCurrency = (request.getPrice() != null && request.getPrice().getCurrency() != null) 
                ? request.getPrice().getCurrency().getValue() : "VND";

        entity.apply(
                request.getTitle(),
                request.getAuthor(),
                request.getAuthorId(),
                request.getPublishedYear(),
                priceAmount,
                priceCurrency
        );
        return repository.saveAndFlush(entity);
    }

    @Transactional
    public void delete(String isbn) {
        BookEntity entity = get(isbn);
        repository.delete(entity);
    }
}
