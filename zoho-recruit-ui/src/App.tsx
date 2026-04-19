import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { Provider } from 'react-redux';
import { store } from './store/store';
import DocumentUploadPage from './pages/DocumentUploadPage';
import './App.css';

function App() {
  return (
    <Provider store={store}>
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<DocumentUploadPage />} />
        </Routes>
      </BrowserRouter>
    </Provider>
  );
}

export default App;
