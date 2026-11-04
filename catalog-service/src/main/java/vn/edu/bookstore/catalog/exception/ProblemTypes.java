package vn.edu.bookstore.catalog.exception;

import java.net.URI;

public class ProblemTypes {
    public static final URI BOOK_NOT_FOUND = URI.create("https://api.bookstore.vn/problems/book-not-found");
    public static final URI VALIDATION_FAILED = URI.create("https://api.bookstore.vn/problems/validation-failed");
}
