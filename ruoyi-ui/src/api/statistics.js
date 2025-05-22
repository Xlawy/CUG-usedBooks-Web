import request from '@/utils/request'

/**
 * 获取所有统计数据
 * @returns {Promise} 返回统计数据的Promise
 */
export function getAllStatistics() {
  return request({
    url: '/system/statistics/all',
    method: 'get'
  })
}

/**
 * 获取用户统计数据
 * @returns {Promise} 返回用户统计数据的Promise
 */
export function getUsersStatistics() {
  return request({
    url: '/system/statistics/users',
    method: 'get'
  })
}

/**
 * 获取订单统计数据
 * @returns {Promise} 返回订单统计数据的Promise
 */
export function getOrdersStatistics() {
  return request({
    url: '/system/statistics/orders',
    method: 'get'
  })
}

/**
 * 获取图书统计数据
 * @returns {Promise} 返回图书统计数据的Promise
 */
export function getBooksStatistics() {
  return request({
    url: '/system/statistics/books',
    method: 'get'
  })
}

/**
 * 获取在售图书统计数据
 * @returns {Promise} 返回在售图书统计数据的Promise
 */
export function getPublishedBooksStatistics() {
  return request({
    url: '/system/statistics/publishedBooks',
    method: 'get'
  })
}

/**
 * 获取月度订单统计数据
 * @param {string} yearMonth 年月，格式：yyyy-MM
 * @returns {Promise} 返回月度订单统计数据的Promise
 */
export function getMonthlyOrdersStats(yearMonth) {
  return request({
    url: `/system/statistics/orders/monthly/${yearMonth}`,
    method: 'get'
  })
}

/**
 * 获取学院图书统计数据
 * @returns {Promise} 返回学院图书统计数据的Promise
 */
export function getCollegeBookStats() {
  return request({
    url: '/system/statistics/books/college',
    method: 'get'
  })
} 