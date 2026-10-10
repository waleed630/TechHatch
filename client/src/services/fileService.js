import api from './api';

const upload = async (path, file) => {
  const formData = new FormData();
  formData.append('file', file);
  const response = await api.post(path, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  // Response format: { success, url, fileName, message }
  return response.data;
};

export const fileService = {
  uploadResume: (file) => upload('/api/v1/files/resume', file),

  uploadProfilePicture: (file) => upload('/api/v1/files/profile-picture', file),

  uploadCompanyLogo: (file) => upload('/api/v1/files/company-logo', file),

  // Full address of a public file (profile picture, company logo) for <img src>
  publicUrl: (url) => (url ? `${import.meta.env.VITE_API_URL}${url}` : null),

  // Resumes need the login token, so they are fetched first: a PDF opens in a new tab,
  // Word files are downloaded
  openFile: async (url, downloadName = 'resume') => {
    // The tab is opened before the request so the browser does not block it as a popup
    const tab = window.open('', '_blank');
    try {
      const response = await api.get(url, { responseType: 'blob' });
      const blob = response.data;
      const blobUrl = URL.createObjectURL(blob);

      if (blob.type === 'application/pdf' && tab) {
        tab.location.href = blobUrl;
      } else {
        if (tab) tab.close();
        const extension = blob.type === 'application/pdf' ? 'pdf' : blob.type === 'application/msword' ? 'doc' : 'docx';
        const link = document.createElement('a');
        link.href = blobUrl;
        link.download = `${downloadName}.${extension}`;
        document.body.appendChild(link);
        link.click();
        link.remove();
      }
      setTimeout(() => URL.revokeObjectURL(blobUrl), 60000);
    } catch (error) {
      if (tab) tab.close();
      throw error;
    }
  },
};
