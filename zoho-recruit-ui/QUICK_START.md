# Quick Start Guide

## 🎯 What Has Been Set Up

A complete React TypeScript UI with Redux, Material-UI, and React Router that communicates with a Spring Boot backend API for document processing.

## ⚡ Quick Commands

### Start Development Server (Frontend)
```powershell
cd d:\workspace\java\hackathon\zoho-recruit-ui
npm run dev
```
→ Open http://localhost:5173 in your browser

### Build for Production
```powershell
npm run build
```

### Run Linting
```powershell
npm lint
```

## 📋 Current Status

✅ **Frontend**: Running on http://localhost:5173
⏳ **Backend**: Awaiting integration (DocumentController.java provided)

## 🔄 Integration Checklist

### Backend Setup (MCP Server)

1. **Copy Controller File**
   ```
   Copy: DocumentController.java
   To: mcp-server-demo/src/main/java/com/mcp/mcp_server/controller/
   ```

2. **Update pom.xml** - Add if missing:
   ```xml
   <dependency>
       <groupId>org.springframework.boot</groupId>
       <artifactId>spring-boot-starter-web</artifactId>
   </dependency>
   ```

3. **Create CORS Configuration**
   - Create: `src/main/java/com/mcp/mcp_server/config/CorsConfig.java`
   - See MCP_SERVER_INTEGRATION.md for content

4. **Update application.yaml**
   ```yaml
   spring:
     servlet:
       multipart:
         max-file-size: 10MB
         max-request-size: 10MB
   ```

5. **Start MCP Server**
   ```bash
   cd d:\workspace\java\hackathon\mcp-server-demo
   mvn spring-boot:run
   ```

## 🧪 Test the Integration

### Option 1: Using the UI
1. Open http://localhost:5173 in browser
2. Enter text or upload a PDF
3. Click Submit
4. Should see success message

### Option 2: Using cURL
```powershell
# Test with text
curl -X POST http://localhost:8080/api/documents/process `
  -F "text=Hello World"

# Test with PDF
curl -X POST http://localhost:8080/api/documents/process `
  -F "pdf=@C:\path\to\document.pdf"
```

## 📁 Project Files

### Frontend Files
```
src/
├── pages/DocumentUploadPage.tsx    # Main page component
├── store/
│   ├── store.ts                   # Redux store config
│   └── documentSlice.ts           # Redux state
├── App.tsx                        # Main app + router
└── main.tsx                       # Entry point

Configuration Files:
- package.json                      # Dependencies
- vite.config.ts                   # Vite configuration
- tsconfig.json                    # TypeScript config
- eslint.config.js                 # Linting rules
```

### Backend File (To Copy)
```
DocumentController.java            # Spring REST controller
```

### Documentation
```
SETUP_GUIDE.md                     # Detailed setup guide
MCP_SERVER_INTEGRATION.md          # Backend integration guide
PROJECT_SUMMARY.md                 # Complete project overview
```

## 🎨 UI Features

### Document Upload Page
- **Text Input Area**: Multi-line text editor
- **PDF Upload**: Drag-and-drop PDF upload
- **Validation**: Ensures at least one input
- **Loading State**: Shows progress while submitting
- **Success/Error Messages**: User feedback
- **Reset Button**: Clear form

### Material-UI Components Used
- Container
- Card & CardContent
- TextField (multiline)
- Button
- Alert
- Paper
- Stack
- Typography
- CircularProgress
- Divider

## 🔧 Configuration

### API Endpoint
Currently configured to: `http://localhost:8080/api/documents/process`

To change the endpoint, edit `src/pages/DocumentUploadPage.tsx`:
```typescript
const response = await fetch('YOUR_API_URL', {
  method: 'POST',
  body: formData,
});
```

## 📚 Available Scripts

```bash
npm run dev       # Start dev server (port 5173)
npm run build     # Production build
npm run preview   # Preview production build
npm lint          # Run ESLint
```

## 🐛 Troubleshooting

### Issue: "Cannot find module '@mui/icons-material/CloudUpload'"
**Solution**: Already installed, but run if needed:
```bash
npm install @mui/icons-material
```

### Issue: Port 5173 already in use
**Solution**: The dev server will use the next available port

### Issue: CORS errors in browser console
**Solution**: Ensure MCP server has CORS configuration enabled

### Issue: "Connection refused" when submitting
**Solution**: Make sure MCP server is running on port 8080

## 📞 Support Resources

- **SETUP_GUIDE.md** - Complete setup documentation
- **MCP_SERVER_INTEGRATION.md** - Backend integration steps
- **PROJECT_SUMMARY.md** - Project overview
- React Docs: https://react.dev
- Material-UI Docs: https://mui.com
- Redux Docs: https://redux.js.org

## ✨ Next Steps

1. **Immediate**: Start the frontend dev server
   ```bash
   npm run dev
   ```

2. **Next**: Set up the backend controller in MCP server
   - Follow steps in MCP_SERVER_INTEGRATION.md

3. **Test**: Use the UI to submit documents
   - Enter text or upload PDF
   - Verify success response

4. **Enhance**: Add more features as needed
   - Additional validation
   - Document storage
   - Processing logic
   - User authentication

## 📊 Architecture

```
┌─────────────────────────────────────────────┐
│         React TypeScript UI                 │
│      (http://localhost:5173)                │
├─────────────────────────────────────────────┤
│  - Document Upload Page                     │
│  - Redux State Management                   │
│  - Material-UI Components                   │
│  - Form Validation                          │
└────────────┬────────────────────────────────┘
             │ HTTP (Fetch API)
             │ POST /api/documents/process
             │
┌────────────▼────────────────────────────────┐
│    Spring Boot MCP Server                   │
│      (http://localhost:8080)                │
├─────────────────────────────────────────────┤
│  - DocumentController                       │
│  - Multipart file handling                  │
│  - Input validation                         │
│  - JSON responses                           │
└─────────────────────────────────────────────┘
```

## 🎉 You're Ready!

Everything is set up and ready to use. Start with:
```bash
npm run dev
```

Then visit http://localhost:5173 to see your application!
