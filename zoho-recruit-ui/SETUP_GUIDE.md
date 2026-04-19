# Zoho Recruit UI - React TypeScript Application

A modern React TypeScript UI for document processing with Material-UI components, Redux state management, and React Router navigation.

## Features

- **React + TypeScript + Vite**: Fast, modern frontend build setup
- **Material-UI (MUI)**: Beautiful, professional UI components
- **Redux Toolkit**: State management for document processing
- **React Router**: Client-side routing
- **Fetch API**: Modern API communication
- **Document Upload**: Support for text input and PDF file uploads
- **Form Validation**: Input validation with user feedback

## Tech Stack

- React 19.2.4
- TypeScript 6.0.2
- Vite 8.0.4
- Material-UI 9.0.0
- Redux Toolkit 2.11.2
- React Router 7.14.1

## Project Structure

```
src/
├── pages/
│   └── DocumentUploadPage.tsx    # Main document upload page
├── store/
│   ├── store.ts                 # Redux store configuration
│   └── documentSlice.ts         # Document state management
├── App.tsx                      # Main app component with routing
├── main.tsx                     # Entry point
└── index.css                    # Global styles
```

## Installation

```bash
npm install
```

## Development

```bash
npm run dev
```

The application will start at `http://localhost:5173`

## Building

```bash
npm run build
```

## Linting

```bash
npm lint
```

## API Integration

The application sends POST requests to:
```
http://localhost:8080/api/documents/process
```

### Request Format

- **Content-Type**: `multipart/form-data`
- **Fields**:
  - `text` (optional): Plain text input
  - `pdf` (optional): PDF file upload

### Expected Response

```json
{
  "status": "success",
  "message": "Document processed successfully",
  "textProcessed": boolean,
  "pdfProcessed": boolean,
  "timestamp": number
}
```

## Backend Setup

To add the DocumentController to your Spring Boot MCP server:

1. Copy the `DocumentController.java` file to:
   ```
   src/main/java/com/mcp/mcp_server/controller/
   ```

2. Ensure your Spring Boot application has the following dependencies in `pom.xml`:
   ```xml
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-web</artifactId>
   </dependency>
   ```

3. Add CORS configuration to your Spring Boot application (if not already present):
   ```java
   @Configuration
   public class CorsConfig implements WebMvcConfigurer {
       @Override
       public void addCorsMappings(CorsRegistry registry) {
           registry.addMapping("/api/**")
               .allowedOrigins("*")
               .allowedMethods("GET", "POST", "PUT", "DELETE")
               .maxAge(3600);
       }
   }
   ```

4. Start your Spring Boot application on port 8080

## Features Overview

### Document Upload Page

The main page allows users to:

1. **Enter Text**: A multi-line text area for user input
2. **Upload PDF**: Drag-and-drop or click to upload PDF files
3. **Submit**: Submit the form with either text or PDF
4. **Reset**: Clear the form

### State Management

Redux is used to manage:
- Text input state
- PDF file selection
- Loading state
- Error messages
- Success notifications

### Form Validation

- At least one input (text or PDF) is required
- PDF files are validated for correct MIME type
- File size is displayed
- User-friendly error messages

## Environment Configuration

For production deployment, update the API endpoint in `src/pages/DocumentUploadPage.tsx`:

```typescript
const response = await fetch('http://your-api-host:8080/api/documents/process', {
  method: 'POST',
  body: formData,
});
```

## Contributing

Feel free to submit issues and enhancement requests!

## License

This project is part of the Zoho Recruit hackathon initiative.
