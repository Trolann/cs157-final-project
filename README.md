# Library Management Application

## Overview
The Library Management System is a comprehensive solution designed to help librarians manage books, borrowers, and the borrowing process. It uses JDBC to connect to a SQLite database, providing a robust yet lightweight solution for small to medium-sized libraries.

This system streamlines library operations by providing tools for inventory management, patron registration, and transaction tracking, allowing librarians to efficiently manage all aspects of a book's lifecycle.

## Features
- **Book Management**: Add, update, and remove books from inventory
- **Author Management**: Track author information and associate with books
- **Borrower Registration**: Register new patrons and issue library cards
- **Circulation Management**: Process checkouts, returns, and track due dates
- **Category Organization**: Organize books by categories
- **Inventory Tracking**: Monitor available copies and total inventory

## Getting Started

### Prerequisites
- Java JDK 11 or higher
- SQLite 3.x

### Installation
1. Clone the repository:
   ```
   git clone https://github.com/yourusername/library-management.git
   ```

2. Navigate to the project directory:
   ```
   cd library-management
   ```

3. Compile the application:
   ```
   javac -d bin src/main/java/libraryapp/*.java
   ```

4. Run the application:
   ```
   java -cp bin:lib/* libraryapp.Main
   ```

### Database Setup
The application will automatically create and initialize a SQLite database file (`library.db`) in the project root directory if one doesn't exist. The database will be seeded with sample data for testing purposes.

## Database Schema

The application uses the following database schema:

- **Books**: book_id, title, isbn, publication_year, publisher, total_copies, available_copies, category_id
- **Authors**: author_id, last_name, first_name, birth_year, biography
- **BookAuthors**: book_id, author_id (junction table for many-to-many relationship)
- **Borrowers**: card_number, last_name, first_name, address, phone, email, registration_date
- **Reservations**: reservation_id, book_id, borrower_id, checkout_date, due_date, return_date, status
- **Categories**: category_id, name, description

## Usage

### Main Interface
The application provides a graphical user interface with the following main sections:
- Book inventory management
- Borrower registration and management
- Checkout and return processing
- Search functionality for books and borrowers

### Common Tasks
- **Adding a new book**: Use the "Add Book" button in the books panel
- **Registering a borrower**: Use the "Add Borrower" button in the borrowers panel
- **Checking out a book**: Select a book and borrower, then use the "Checkout" button
- **Returning a book**: Select a borrowed book and use the "Return" button

## Development

### Technology Stack
- Java with JavaFX for the user interface
- JDBC 2.0 for database connectivity
- SQLite for data storage

### Project Structure
- `src/main/java/libraryapp/`: Contains all Java source files
- `lib/`: Contains required libraries and dependencies
- `resources/`: Contains application resources (icons, etc.)

## Team Members
- Trevor Mathisen
- Jason Tobin
- Viet Nguyen

## License
This project is licensed under the MIT License - see the LICENSE file for details.
