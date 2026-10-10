import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';
import { profileService } from '../../services/profileService';
import { fileService } from '../../services/fileService';
import Navbar from '../../components/common/Navbar';
import { Button } from '../../components/ui/button';
import { Input } from '../../components/ui/input';
import { Label } from '../../components/ui/label';
import { Textarea } from '../../components/ui/textarea';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../../components/ui/select';
import { COMPANY_SIZES } from '../../utils/constants';
import toast from 'react-hot-toast';

const RecruiterProfile = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [profileExists, setProfileExists] = useState(false);
  const [logoUrl, setLogoUrl] = useState('');
  const [uploading, setUploading] = useState(false);
  const [formData, setFormData] = useState({
    companyName: '',
    companyDescription: '',
    phone: '',
    companyWebsite: '',
    companySize: '',
    industry: '',
    location: '',
  });

  useEffect(() => {
    fetchProfile();
  }, []);

  const fetchProfile = async () => {
    try {
      const data = await profileService.getRecruiterProfile(user.userId);
      setFormData({
        companyName: data.companyName || '',
        companyDescription: data.companyDescription || '',
        phone: data.phone || '',
        companyWebsite: data.companyWebsite || '',
        companySize: data.companySize || '',
        industry: data.industry || '',
        location: data.location || '',
      });
      setProfileExists(true);
      setLogoUrl(data.companyLogoUrl || '');
    } catch (error) {
      // Profile doesn't exist yet
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
  };

  const handleLogoUpload = async (e) => {
    const file = e.target.files[0];
    e.target.value = '';
    if (!file) return;

    if (file.size > 2 * 1024 * 1024) {
      toast.error('Logo must be smaller than 2 MB');
      return;
    }

    setUploading(true);
    try {
      const data = await fileService.uploadCompanyLogo(file);
      setLogoUrl(data.url);
      toast.success(data.message || 'Company logo uploaded successfully');
    } catch (error) {
      toast.error(error.response?.data?.message || 'Failed to upload logo');
    } finally {
      setUploading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSaving(true);

    try {
      await profileService.updateRecruiterProfile(formData);
      toast.success('Profile updated successfully!');
      navigate('/recruiter/dashboard');
    } catch (error) {
      toast.error(error.response?.data?.message || 'Failed to update profile');
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-background">
        <Navbar />
        <div className="flex min-h-[calc(100vh-80px)] items-center justify-center">
          <div className="text-xl">Loading...</div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background">
      <Navbar />
      <div className="container mx-auto px-6 py-8">
        <div className="mx-auto max-w-3xl">
          <h1 className="mb-2 text-3xl font-bold text-foreground">Company Profile</h1>
          <p className="mb-8 text-muted-foreground">
            Complete your company profile to start posting jobs
          </p>

          <div className="mb-6 space-y-3 rounded-lg border bg-card p-8">
            <h2 className="text-xl font-semibold text-foreground">Company Logo</h2>
            {!profileExists ? (
              <p className="text-sm text-muted-foreground">
                Save your company profile first, then you can upload your logo here.
              </p>
            ) : (
              <div className="flex items-center gap-6">
                {logoUrl ? (
                  <img
                    src={fileService.publicUrl(logoUrl)}
                    alt="Company logo"
                    className="h-24 w-24 rounded-lg border object-contain"
                  />
                ) : (
                  <div className="flex h-24 w-24 items-center justify-center rounded-lg border bg-muted text-xs text-muted-foreground">
                    No logo
                  </div>
                )}
                <div className="flex-1 space-y-2">
                  <Label htmlFor="logo">{logoUrl ? 'Change Logo' : 'Upload Logo'}</Label>
                  <Input
                    id="logo"
                    type="file"
                    accept=".png,.jpg,.jpeg,.webp"
                    onChange={handleLogoUpload}
                    disabled={uploading}
                  />
                  <p className="text-xs text-muted-foreground">PNG, JPG or WEBP, up to 2 MB</p>
                </div>
              </div>
            )}
          </div>

          <form onSubmit={handleSubmit} className="space-y-6 rounded-lg border bg-card p-8">
            <div className="space-y-2">
              <Label htmlFor="companyName">Company Name *</Label>
              <Input
                id="companyName"
                name="companyName"
                value={formData.companyName}
                onChange={handleChange}
                required
                placeholder="Write company name"
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="companyDescription">Company Description *</Label>
              <Textarea
                id="companyDescription"
                name="companyDescription"
                value={formData.companyDescription}
                onChange={handleChange}
                required
                placeholder="Write about your company"
                rows={4}
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="phone">Phone Number</Label>
              <Input
                id="phone"
                name="phone"
                value={formData.phone}
                onChange={handleChange}
                placeholder="Enter phone number"
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="companyWebsite">Website</Label>
              <Input
                id="companyWebsite"
                name="companyWebsite"
                value={formData.companyWebsite}
                onChange={handleChange}
                placeholder="Enter website link"
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="companySize">Company Size *</Label>
              <Select value={formData.companySize} onValueChange={(value) => setFormData({ ...formData, companySize: value })}>
                <SelectTrigger>
                  <SelectValue placeholder="Select company size" />
                </SelectTrigger>
                <SelectContent>
                  {COMPANY_SIZES.map((size) => (
                    <SelectItem key={size} value={size}>
                      {size} employees
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
            </div>

            <div className="space-y-2">
              <Label htmlFor="industry">Industry</Label>
              <Input
                id="industry"
                name="industry"
                value={formData.industry}
                onChange={handleChange}
                placeholder="Add industry type"
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="location">Location *</Label>
              <Input
                id="location"
                name="location"
                value={formData.location}
                onChange={handleChange}
                required
                placeholder="Enter company location"
              />
            </div>

            <div className="flex space-x-4">
              <Button type="submit" disabled={saving}>
                {saving ? 'Saving...' : 'Save Profile'}
              </Button>
              <Button
                type="button"
                variant="outline"
                onClick={() => navigate('/recruiter/dashboard')}
              >
                Cancel
              </Button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

export default RecruiterProfile;
