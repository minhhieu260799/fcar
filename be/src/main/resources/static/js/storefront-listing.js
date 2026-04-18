(function () {
    var input = document.getElementById('storefrontCarSearchInput');
    var mount = document.getElementById('storefrontCarListingMount');
    var statusEl = document.getElementById('storefrontCarSearchStatus');
    var clearBtn = document.getElementById('storefrontFilterClear');
    var inStockEl = document.getElementById('storefrontFilterInStock');

    if (!mount) {
        return;
    }

    var home = mount.getAttribute('data-car-list-on-home') === 'true';
    var size = parseInt(mount.getAttribute('data-page-size') || '12', 10) || 12;
    var searchDebounceMs = 350;
    var searchTimer = null;

    function collectCsv(selector) {
        var arr = [];
        document.querySelectorAll(selector + ':checked').forEach(function (cb) {
            arr.push(cb.value);
        });
        return arr.join(',');
    }

    function buildFragmentUrl() {
        var params = new URLSearchParams();
        var q = input ? input.value.trim() : '';
        if (q) {
            params.set('q', q);
        }
        var b = collectCsv('.storefront-filter-brand');
        if (b) {
            params.set('brands', b);
        }
        var m = collectCsv('.storefront-filter-model');
        if (m) {
            params.set('models', m);
        }
        var s = collectCsv('.storefront-filter-segment');
        if (s) {
            params.set('segments', s);
        }
        if (inStockEl && inStockEl.checked) {
            params.set('inStock', 'true');
        }
        params.set('page', '0');
        params.set('size', String(size));
        params.set('carListOnHome', home ? 'true' : 'false');
        return '/fragments/storefront-car-listing?' + params.toString();
    }

    function syncBrowserUrl() {
        var path = home ? '/' : '/cars';
        var u = new URL(window.location.origin + path);
        var q = input ? input.value.trim() : '';
        if (q) {
            u.searchParams.set('q', q);
        } else {
            u.searchParams.delete('q');
        }
        var b = collectCsv('.storefront-filter-brand');
        if (b) {
            u.searchParams.set('brands', b);
        } else {
            u.searchParams.delete('brands');
        }
        var m = collectCsv('.storefront-filter-model');
        if (m) {
            u.searchParams.set('models', m);
        } else {
            u.searchParams.delete('models');
        }
        var s = collectCsv('.storefront-filter-segment');
        if (s) {
            u.searchParams.set('segments', s);
        } else {
            u.searchParams.delete('segments');
        }
        if (inStockEl && inStockEl.checked) {
            u.searchParams.set('inStock', 'true');
        } else {
            u.searchParams.delete('inStock');
        }
        u.searchParams.delete('page');
        history.replaceState(null, '', u.pathname + u.search);
    }

    function updateStatusFromDoc(htmlDoc) {
        if (!statusEl) {
            return;
        }
        var q = input ? input.value.trim() : '';
        var hasFilters =
            !!q ||
            document.querySelectorAll('.storefront-filter-brand:checked').length > 0 ||
            document.querySelectorAll('.storefront-filter-model:checked').length > 0 ||
            document.querySelectorAll('.storefront-filter-segment:checked').length > 0 ||
            (inStockEl && inStockEl.checked);
        if (!hasFilters) {
            statusEl.textContent = '';
            return;
        }
        var meta = htmlDoc.querySelector('.storefront-listing-meta');
        var total = meta ? parseInt(meta.getAttribute('data-total-elements') || '0', 10) : 0;
        statusEl.textContent =
            total === 0 ? 'Không có mẫu xe phù hợp' : 'Tìm thấy ' + total + ' mẫu xe phù hợp';
    }

    function setLoading(on) {
        if (input) {
            input.classList.toggle('home-catalog__search-input--loading', on);
            input.setAttribute('aria-busy', on ? 'true' : 'false');
        }
    }

    function fetchListing() {
        setLoading(true);
        fetch(buildFragmentUrl(), {
            method: 'GET',
            credentials: 'same-origin',
            headers: { Accept: 'text/html' }
        })
            .then(function (res) {
                if (!res.ok) {
                    throw new Error('fetch');
                }
                return res.text();
            })
            .then(function (html) {
                mount.innerHTML = html;
                var parser = new DOMParser();
                var doc = parser.parseFromString(html, 'text/html');
                updateStatusFromDoc(doc);
                syncBrowserUrl();
                setLoading(false);
            })
            .catch(function () {
                setLoading(false);
                if (statusEl) {
                    statusEl.textContent = 'Không tải được kết quả. Thử lại sau.';
                }
                /* Tránh hiển thị kết quả cũ (SSR) khi request lỗi — dễ gây hiểu nhầm với bộ lọc mới */
                if (mount) {
                    mount.innerHTML =
                        '<div class="storefront-car-list-empty text-center py-5 px-3 rounded-4 border border-dashed border-warning bg-warning bg-opacity-10">' +
                        '<p class="small text-body-secondary mb-0">Không tải được kết quả. Bạn có thể tải lại trang hoặc thử lại sau.</p></div>';
                }
            });
    }

    function scheduleSearchFetch() {
        if (searchTimer) {
            clearTimeout(searchTimer);
        }
        searchTimer = setTimeout(function () {
            searchTimer = null;
            fetchListing();
        }, searchDebounceMs);
    }

    function applyCascadeVisibility() {
        var brandsChecked = [];
        document.querySelectorAll('.storefront-filter-brand:checked').forEach(function (cb) {
            brandsChecked.push(cb.value);
        });
        var modelsChecked = [];
        document.querySelectorAll('.storefront-filter-model:checked').forEach(function (cb) {
            modelsChecked.push(cb.value);
        });

        document.querySelectorAll('.storefront-filter-model-wrap').forEach(function (el) {
            var bid = el.getAttribute('data-brand-id');
            var show = brandsChecked.length === 0 || brandsChecked.indexOf(bid) !== -1;
            el.classList.toggle('d-none', !show);
        });

        document.querySelectorAll('.storefront-filter-segment-wrap').forEach(function (el) {
            var mid = el.getAttribute('data-model-id');
            var show = modelsChecked.length === 0 || modelsChecked.indexOf(mid) !== -1;
            el.classList.toggle('d-none', !show);
        });
    }

    if (input) {
        input.addEventListener('input', function () {
            scheduleSearchFetch();
        });
        input.addEventListener('search', function () {
            if (searchTimer) {
                clearTimeout(searchTimer);
                searchTimer = null;
            }
            fetchListing();
        });
    }

    document.querySelectorAll('.storefront-filter-brand, .storefront-filter-model, .storefront-filter-segment').forEach(function (el) {
        el.addEventListener('change', function () {
            applyCascadeVisibility();
            fetchListing();
        });
    });

    if (inStockEl) {
        inStockEl.addEventListener('change', fetchListing);
    }

    if (clearBtn) {
        clearBtn.addEventListener('click', function () {
            if (input) {
                input.value = '';
            }
            document.querySelectorAll('.storefront-filter-brand, .storefront-filter-model, .storefront-filter-segment').forEach(function (cb) {
                cb.checked = false;
            });
            if (inStockEl) {
                inStockEl.checked = false;
            }
            applyCascadeVisibility();
            fetchListing();
        });
    }

    applyCascadeVisibility();
})();
