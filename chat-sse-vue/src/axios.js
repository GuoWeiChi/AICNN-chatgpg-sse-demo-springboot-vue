import Axios from 'axios';

const axiosInstance = Axios.create({
    baseURL: 'http://127.0.0.1',
    // 其他配置
});

export default axiosInstance;