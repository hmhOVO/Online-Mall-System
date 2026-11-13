import axios from "axios";
import {ElMessage} from "element-plus";
import router from "@/router/index.js";

const request = axios.create({
    baseURL: '/api',
    timeout: 120000
});

let isRefreshing = false;

request.interceptors.request.use(config => {
    if (config.data instanceof FormData) {
        delete config.headers['Content-Type'];
    } else {
        config.headers['Content-Type'] = 'application/json;charset=utf-8';
    }
    let userStr = localStorage.getItem('code_user');
    if (userStr) {
        try {
            let user = JSON.parse(userStr);
            if (user.token) {
                config.headers['token'] = user.token;
            }
        } catch (e) {
            console.error('解析用户信息失败', e);
            localStorage.removeItem('code_user');
        }
    }
    return config;
}, error => {
    return Promise.reject(error);
});

const handleAuthError = () => {
    if (isRefreshing) return;
    isRefreshing = true;
    ElMessage.error('登录已过期，请重新登录');
    localStorage.removeItem('code_user');
    router.push('/login').finally(() => {
        setTimeout(() => {
            isRefreshing = false;
        }, 3000);
    });
};

request.interceptors.response.use(
    response => {
        if (response.config.responseType === 'blob') {
            const contentType = response.headers['content-type'] || '';
            if (contentType.includes('application/json')) {
                return new Promise((resolve, reject) => {
                    const reader = new FileReader();
                    reader.onload = () => {
                        try {
                            const res = JSON.parse(reader.result);
                            if (res.code === '401') {
                                handleAuthError();
                                reject(new Error(res.msg || '未登录'));
                            } else if (res.code === '403') {
                                ElMessage.error(res.msg || '无权限访问');
                                reject(new Error(res.msg || '无权限'));
                            } else {
                                ElMessage.error(res.msg || '操作失败');
                                reject(new Error(res.msg || '操作失败'));
                            }
                        } catch (e) {
                            reject(new Error('导出失败'));
                        }
                    };
                    reader.onerror = () => reject(new Error('导出失败'));
                    reader.readAsText(response.data);
                });
            }
            return response.data;
        }
        let res = response.data;
        if (typeof res === 'string') {
            res = res ? JSON.parse(res) : res;
        }
        if (res.code === '401') {
            handleAuthError();
            return Promise.reject(new Error(res.msg || '未登录'));
        }
        if (res.code === '403') {
            ElMessage.error(res.msg || '无权限访问');
            return Promise.reject(new Error(res.msg || '无权限'));
        }
        if (res.code !== '200') {
            ElMessage.error(res.msg || '操作失败');
            return Promise.reject(new Error(res.msg || '操作失败'));
        }
        return res;
    },
    error => {
        if (error.response) {
            if (error.response.status === 401) {
                handleAuthError();
            } else if (error.response.status === 403) {
                ElMessage.error('无权限访问');
            } else if (error.response.status === 404) {
                ElMessage.error('未找到请求接口');
            } else if (error.response.status === 500) {
                ElMessage.error('系统异常，请查看后端控制台');
            } else {
                ElMessage.error(error.message || '网络异常');
            }
        } else {
            ElMessage.error('网络异常');
        }
        return Promise.reject(error);
    }
);

export default request;
