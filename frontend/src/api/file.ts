import service from './request'
import type { ApiResponse } from '@/types/api'

/**
 * 上传文件
 */
export function uploadFile(file: File): Promise<ApiResponse<string>> {
  const formData = new FormData()
  formData.append('file', file)
  
  return service.post('/files/upload', formData, {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  })
}

