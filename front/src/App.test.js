import { render, screen } from '@testing-library/react';
import App from './App';

test('renders login screen when unauthenticated', () => {
  localStorage.clear();
  render(<App />);
  const title = screen.getByText(/shar platform/i);
  expect(title).toBeInTheDocument();
});
