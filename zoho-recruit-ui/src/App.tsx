import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { Provider } from 'react-redux';
import { CssBaseline, Box } from '@mui/material';
import { store } from './store/store';
import DocumentUploadPage from './pages/DocumentUploadPage';
import './App.css';

function App() {
  return (
    <Provider store={store}>
      <CssBaseline />
      <BrowserRouter>
        <Box sx={{ minHeight: '100vh', backgroundColor: '#fafafa' }}>
          <Routes>
            <Route path="/" element={<DocumentUploadPage />} />
          </Routes>
        </Box>
      </BrowserRouter>
    </Provider>
  );
}

export default App;
