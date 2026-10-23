export function getCookie(name) {
  const match = document.cookie.match(new RegExp('(^| )' + name + '=([^;]+)'))
  return match ? decodeURIComponent(match[2]) : ''
}

export function setCookie(name, value, { expires } = {}) {
  let cookie = `${name}=${encodeURIComponent(value)}; Path=/; SameSite=Strict;`
  if (expires) {
    cookie += ` Expires=${expires};`
  }
  document.cookie = cookie
}

export function clearCookie(name) {
  setCookie(name, '', { expires: 'Thu, 01 Jan 1970 00:00:01 GMT' })
}
