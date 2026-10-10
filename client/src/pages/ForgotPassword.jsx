import { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Eye, EyeOff } from 'lucide-react';
import { authService } from '../services/authService';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Label } from '../components/ui/label';
import Navbar from '../components/common/Navbar';
import toast from 'react-hot-toast';

const ForgotPassword = () => {
  const [email, setEmail] = useState('');
  const [otpSent, setOtpSent] = useState(false);
  const [otpCode, setOtpCode] = useState('');
  const [password, setPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [resetting, setResetting] = useState(false);
  const [resendTimer, setResendTimer] = useState(0);
  const navigate = useNavigate();

  // Timer effect for resend OTP
  useEffect(() => {
    let interval = null;
    if (resendTimer > 0) {
      interval = setInterval(() => {
        setResendTimer((prev) => prev - 1);
      }, 1000);
    }
    return () => {
      if (interval) clearInterval(interval);
    };
  }, [resendTimer]);

  const formatTimer = (seconds) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  };

  const sendOTP = async () => {
    setLoading(true);

    try {
      const response = await authService.forgotPassword(email);
      setOtpSent(true);
      setResendTimer(60); // Start 1-minute timer
      toast.success(response.message || 'OTP sent to your email');
    } catch (error) {
      toast.error(error.response?.data?.message || error.response?.data?.error || 'Failed to send OTP');
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    await sendOTP();
  };

  const handleResendOTP = async () => {
    if (resendTimer > 0) {
      return;
    }
    await sendOTP();
  };

  const handleReset = async (e) => {
    e.preventDefault();

    if (!otpCode || otpCode.length !== 6) {
      toast.error('Please enter a 6-digit OTP');
      return;
    }

    if (password.length < 6) {
      toast.error('Password must be at least 6 characters');
      return;
    }

    if (password !== confirmPassword) {
      toast.error('Passwords do not match');
      return;
    }

    setResetting(true);

    try {
      await authService.resetPassword(email, otpCode, password);
      toast.success('Password reset successfully! Please login.');
      navigate('/login');
    } catch (error) {
      toast.error(error.response?.data?.message || error.response?.data?.error || 'Failed to reset password');
    } finally {
      setResetting(false);
    }
  };

  return (
    <div className="min-h-screen bg-background">
      <Navbar />
      <div className="flex min-h-[calc(100vh-80px)] items-center justify-center px-6">
        <div className="w-full max-w-md space-y-8 rounded-lg border bg-card p-8 shadow-lg">
          <div className="text-center">
            <h2 className="text-3xl font-bold text-foreground">Forgot Password</h2>
            <p className="mt-2 text-muted-foreground">
              {otpSent ? 'Enter the OTP and choose a new password' : 'We will send an OTP to your email'}
            </p>
          </div>

          {!otpSent ? (
            <form onSubmit={handleSubmit} className="space-y-6">
              <div className="space-y-2">
                <Label htmlFor="email">Email Address</Label>
                <Input
                  id="email"
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                  placeholder="you@example.com"
                  className="tracking-wider placeholder:text-muted"
                />
              </div>

              <Button type="submit" className="w-full" disabled={loading}>
                {loading ? 'Sending OTP...' : 'Send OTP'}
              </Button>
            </form>
          ) : (
            <form onSubmit={handleReset} className="space-y-6">
              <div className="space-y-2">
                <Label htmlFor="otp">Enter OTP</Label>
                <p className="text-sm text-muted-foreground">
                  We've sent a verification code to {email}
                </p>
                <Input
                  id="otp"
                  type="text"
                  value={otpCode}
                  onChange={(e) => setOtpCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
                  required
                  placeholder="000000"
                  className="text-center text-2xl font-bold tracking-widest placeholder:text-muted"
                  maxLength={6}
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="password">New Password</Label>
                <div className="relative">
                  <Input
                    id="password"
                    type={showPassword ? 'text' : 'password'}
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    required
                    placeholder="••••••••"
                    className="font-bold tracking-widest placeholder:text-muted pr-10"
                  />
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    className="absolute right-0 top-0 h-full px-3 py-2 hover:bg-transparent"
                    onClick={() => setShowPassword(!showPassword)}
                  >
                    {showPassword ? (
                      <EyeOff className="h-4 w-4 text-muted-foreground" />
                    ) : (
                      <Eye className="h-4 w-4 text-muted-foreground" />
                    )}
                  </Button>
                </div>
              </div>

              <div className="space-y-2">
                <Label htmlFor="confirmPassword">Confirm New Password</Label>
                <Input
                  id="confirmPassword"
                  type={showPassword ? 'text' : 'password'}
                  value={confirmPassword}
                  onChange={(e) => setConfirmPassword(e.target.value)}
                  required
                  placeholder="••••••••"
                  className="font-bold tracking-widest placeholder:text-muted"
                />
              </div>

              <Button type="submit" className="w-full" disabled={resetting}>
                {resetting ? 'Resetting...' : 'Reset Password'}
              </Button>

              <Button
                type="button"
                variant="outline"
                onClick={handleResendOTP}
                disabled={resendTimer > 0 || loading}
                className="w-full"
              >
                {resendTimer > 0 ? `Resend OTP (${formatTimer(resendTimer)})` : 'Resend OTP'}
              </Button>
            </form>
          )}

          <div className="text-center text-sm">
            <span className="text-muted-foreground">Remember your password? </span>
            <Link to="/login" className="font-medium text-primary hover:underline">
              Login
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
};

export default ForgotPassword;
