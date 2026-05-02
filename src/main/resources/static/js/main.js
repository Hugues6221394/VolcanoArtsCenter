/* ═══════════════════════════════════════════════════════════════
   VOLCANO ARTS CENTER — Main JavaScript
   Navigation, scroll animations, and interactive behaviors
   ═══════════════════════════════════════════════════════════════ */

document.addEventListener('DOMContentLoaded', () => {
    const root = document.documentElement;
    const themeToggle = document.getElementById('theme-toggle');
    const themeToggleLabel = document.getElementById('theme-toggle-label');
    const themeToggleIcon = document.getElementById('theme-toggle-icon');

    function applyTheme(theme) {
        root.setAttribute('data-theme', theme);
        if (themeToggleLabel) themeToggleLabel.textContent = theme === 'light' ? 'Light' : 'Dark';
        if (themeToggleIcon) themeToggleIcon.textContent = theme === 'light' ? '☀️' : '🌙';
    }

    const initialTheme = root.getAttribute('data-theme') === 'light' ? 'light' : 'dark';
    applyTheme(initialTheme);

    if (themeToggle) {
        themeToggle.addEventListener('click', () => {
            const next = root.getAttribute('data-theme') === 'light' ? 'dark' : 'light';
            applyTheme(next);
            try {
                localStorage.setItem('vac-theme', next);
            } catch (e) {
                // Ignore storage failures
            }
        });
    }

    // ── Navigation scroll effect ──
    const nav = document.querySelector('.nav');
    if (nav) {
        const onScroll = () => {
            nav.classList.toggle('scrolled', window.scrollY > 50);
        };
        window.addEventListener('scroll', onScroll, { passive: true });
        onScroll();
    }

    // ── Mobile menu toggle ──
    const navToggle = document.querySelector('.nav__toggle');
    const navLinks = document.querySelector('.nav__links');
    if (navToggle && navLinks) {
        navToggle.addEventListener('click', () => {
            navLinks.classList.toggle('open');
            navToggle.classList.toggle('active');
        });

        // Close menu on link click
        navLinks.querySelectorAll('a').forEach(link => {
            link.addEventListener('click', () => {
                navLinks.classList.remove('open');
                navToggle.classList.remove('active');
            });
        });
    }

    // ── Scroll-triggered animations ──
    const observerOptions = {
        root: null,
        rootMargin: '0px 0px -80px 0px',
        threshold: 0.1
    };

    const scrollObserver = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add('visible');
                scrollObserver.unobserve(entry.target);
            }
        });
    }, observerOptions);

    document.querySelectorAll('.animate-on-scroll').forEach(el => {
        scrollObserver.observe(el);
    });

    // ── Smooth scroll for anchor links ──
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', (e) => {
            const targetId = anchor.getAttribute('href');
            if (targetId === '#') return;
            const target = document.querySelector(targetId);
            if (target) {
                e.preventDefault();
                target.scrollIntoView({
                    behavior: 'smooth',
                    block: 'start'
                });
            }
        });
    });

    // ── Counter animation for impact numbers ──
    const counters = document.querySelectorAll('[data-count]');
    if (counters.length) {
        const counterObserver = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    animateCounter(entry.target);
                    counterObserver.unobserve(entry.target);
                }
            });
        }, { threshold: 0.5 });

        counters.forEach(el => counterObserver.observe(el));
    }

    function animateCounter(el) {
        const target = parseInt(el.getAttribute('data-count'), 10);
        const suffix = el.getAttribute('data-suffix') || '';
        const prefix = el.getAttribute('data-prefix') || '';
        const duration = 2000;
        const start = performance.now();

        function update(now) {
            const elapsed = now - start;
            const progress = Math.min(elapsed / duration, 1);
            // Ease out cubic
            const eased = 1 - Math.pow(1 - progress, 3);
            const current = Math.round(eased * target);
            el.textContent = prefix + current.toLocaleString() + suffix;
            if (progress < 1) {
                requestAnimationFrame(update);
            }
        }

        requestAnimationFrame(update);
    }

    // ── Active nav link highlighting ──
    const currentPath = window.location.pathname;
    document.querySelectorAll('.nav__links a').forEach(link => {
        const href = link.getAttribute('href');
        if (href === currentPath || (href !== '/' && currentPath.startsWith(href))) {
            link.classList.add('active');
        }
    });

    const parallaxItems = document.querySelectorAll('[data-parallax]');
    if (parallaxItems.length) {
        const onParallax = () => {
            const y = window.scrollY || 0;
            parallaxItems.forEach((el) => {
                const speed = Number(el.getAttribute('data-parallax')) || 0.12;
                el.style.transform = `translate3d(0, ${Math.round(y * speed)}px, 0)`;
            });
        };
        window.addEventListener('scroll', onParallax, { passive: true });
        onParallax();
    }

});
