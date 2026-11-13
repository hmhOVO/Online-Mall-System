import request from '@/utils/request.js';

export function downloadFile(id) {
  return request({
    url: '/file/' + id + '/download',
    method: 'get',
    responseType: 'blob'
  });
}

export function previewFile(id) {
  return request({
    url: '/file/' + id + '/preview',
    method: 'get',
    responseType: 'blob'
  });
}

export function getFileInfo(id) {
  return request.get('/file/' + id + '/info');
}

export function deleteFile(id) {
  return request.delete('/file/' + id);
}

export function listProjectFiles(projectId) {
  return request.get('/file/project/' + projectId);
}

export function uploadProjectFile(projectId, file) {
  const formData = new FormData();
  formData.append('file', file);
  return request({
    url: '/file/project/' + projectId + '/upload',
    method: 'post',
    data: formData
  });
}
