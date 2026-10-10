import api from './api';

export const applicationService = {
  applyToJob: async (jobId, coverLetter) => {
    const response = await api.post('/api/v1/applications', {
      jobId,
      coverLetter,
    });
    return response.data;
  },

  getMyApplications: async () => {
    const response = await api.get('/api/v1/applications/my-applications'
    );
    return response.data.content;
  },

  // One page of my applications with paging info: { content, totalPages, totalElements }
  getMyApplicationsPage: async (page = 0) => {
    const response = await api.get('/api/v1/applications/my-applications', {
      params: { page },
    });
    return response.data;
  },

  // My application for one job, null when I have not applied
  getMyApplicationForJob: async (jobId) => {
    const response = await api.get(`/api/v1/applications/my-applications/job/${jobId}`);
    return response.status === 204 ? null : response.data;
  },

  // Totals over all of my applications: { total, pending, interviews }
  getMyStats: async () => {
    const response = await api.get('/api/v1/applications/my-stats');
    return response.data;
  },

  getApplicationsForJob: async (jobId, params) => {
    const response = await api.get(`/api/v1/applications/job/${jobId}`,{params});
    return response.data;
  },

  updateApplicationStatus: async (id, status, recruiterNotes) => {
    const response = await api.patch(`/api/v1/applications/${id}/status`, {
      status,
      recruiterNotes,
    });
    return response.data;
  },
};
