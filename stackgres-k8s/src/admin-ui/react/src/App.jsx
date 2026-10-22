import { Routes, Route } from 'react-router-dom'
import Placeholder from './components/Placeholder'

export default function App() {
  return (
    <Routes>
      <Route path="*" element={<Placeholder />} />
    </Routes>
  )
}
