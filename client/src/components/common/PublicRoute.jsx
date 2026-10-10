import { useAuth } from '../../context/AuthContext';
import { Navigate } from 'react-router-dom';

// Pages for visitors (landing, login, register): a logged in user is sent to their dashboard
const PublicRoute = ({ children }) => {
    const { user, loading} = useAuth();

    if (loading) {
        return (
          <div className="flex min-h-screen items-center justify-center">
            <div className="text-xl">Loading...</div>
          </div>
        );
      }

    if (user?.role === 'CANDIDATE') {
        return <Navigate to="/candidate/dashboard" replace/>;
    }

    if (user?.role === 'RECRUITER') {
        return <Navigate to="/recruiter/dashboard" replace/>;
    }

    return children;
};

export default PublicRoute
