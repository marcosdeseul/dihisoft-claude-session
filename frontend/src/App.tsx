import { BrowserRouter, Route, Routes } from 'react-router-dom';
import SignupPage from './pages/SignupPage';

function HomePlaceholder() {
  return <h1>React OK</h1>;
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/signup" element={<SignupPage />} />
        <Route path="*" element={<HomePlaceholder />} />
      </Routes>
    </BrowserRouter>
  );
}
