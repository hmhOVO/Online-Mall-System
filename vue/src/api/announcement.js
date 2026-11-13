import request from '@/utils/request.js';

export const getAnnouncementList = (params) => {
  return request.get('/admin/announcement/list', { params });
};

export const getAnnouncementDetail = (id) => {
  return request.get(`/admin/announcement/${id}`);
};

export const createAnnouncement = (data) => {
  return request.post('/admin/announcement', data);
};

export const updateAnnouncement = (id, data) => {
  return request.put(`/admin/announcement/${id}`, data);
};

export const deleteAnnouncement = (id) => {
  return request.delete(`/admin/announcement/${id}`);
};

export const publishAnnouncement = (id) => {
  return request.put(`/admin/announcement/${id}/publish`);
};

export const offlineAnnouncement = (id) => {
  return request.put(`/admin/announcement/${id}/offline`);
};

export const toggleAnnouncementTop = (id) => {
  return request.put(`/admin/announcement/${id}/toggle-top`);
};

export const getPublishedAnnouncements = (params) => {
  return request.get('/announcement/published', { params });
};

export const getPublishedAnnouncementDetail = (id) => {
  return request.get(`/announcement/${id}`);
};
