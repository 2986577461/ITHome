import { onMounted, onUnmounted, nextTick } from 'vue';

export function useScrollReveal() {
  let observer = null;

  function observe() {
    if (observer) observer.disconnect();
    observer = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            entry.target.classList.add('visible');
            observer.unobserve(entry.target);
          }
        });
      },
      { threshold: 0.1, rootMargin: '0px 0px -40px 0px' },
    );
    document
      .querySelectorAll('.reveal, .reveal-scale, .reveal-left, .reveal-right')
      .forEach((el) => observer.observe(el));
  }

  onMounted(async () => {
    await nextTick();
    observe();
  });

  onUnmounted(() => {
    if (observer) observer.disconnect();
  });

  return { observe };
}
