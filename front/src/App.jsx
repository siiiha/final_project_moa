import { Route, Routes } from 'react-router-dom';
import Layout from './components/Layout';
import HomePage from './pages/HomePage';
import NotFound from './pages/NotFound';

function App() {
  return (
    <Routes>
      {/* Layout 안의 <Outlet /> 자리에 아래 페이지들이 바뀌어 들어간다 */}
      <Route element={<Layout />}>
        <Route path="/" element={<HomePage />} />
      </Route>
      <Route path="*" element={<NotFound />} />
    </Routes>
  );
}

export default App;
