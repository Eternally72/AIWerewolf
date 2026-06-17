export type ImageCache = Map<string, HTMLImageElement>

const cache: ImageCache = new Map()

export function imageCache() {
  return cache
}

export function preloadImages(urls: string[]) {
  return Promise.allSettled(urls.map(preloadImage))
}

export function preloadImage(url: string) {
  const cached = cache.get(url)
  if (cached?.complete) {
    return Promise.resolve(cached)
  }
  return new Promise<HTMLImageElement>((resolve, reject) => {
    const image = cached ?? new Image()
    cache.set(url, image)
    image.decoding = 'async'
    image.onload = () => resolve(image)
    image.onerror = () => reject(new Error(`Image load failed: ${url}`))
    if (!cached) {
      image.src = url
    }
  })
}
