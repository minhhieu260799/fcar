/** Phân công: Hiệp Hiếu — so sánh xe & yêu thích từ danh sách (fetch). */
function getCompareList() {
    const raw = localStorage.getItem('compareCars');
    if (!raw) return [];
    try {
        const arr = JSON.parse(raw);
        if (!Array.isArray(arr)) return [];
        return arr.map(function (x) {
            return String(x);
        });
    } catch (e) {
        return [];
    }
}

function saveCompareList(list) {
    localStorage.setItem('compareCars', JSON.stringify(list.map(String)));
}

function addToCompare(button) {
    const carId = String(button.getAttribute('data-car-id'));
    let list = getCompareList();
    if (list.includes(carId)) {
        alert('Xe này đã có trong danh sách so sánh.');
        return;
    }
    if (list.length >= 3) {
        alert('Đã tồn tại 3 xe trong mục so sánh, quý khách vui lòng bỏ chọn 1 xe');
        return;
    }
    list.push(carId);
    saveCompareList(list);
    alert('Đã thêm xe vào danh sách so sánh.');
}

function addToCompareByIdFromButton(button) {
    addToCompareById(button.getAttribute('data-car-id'));
}

function addToCompareByIdFromModal(button) {
    addToCompareById(button.getAttribute('data-car-id'));
}

function addToCompareById(carId) {
    if (carId == null || carId === '') return;
    carId = String(carId);
    let list = getCompareList();
    if (list.includes(carId)) {
        window.location.href = '/cars/compare?ids=' + encodeURIComponent(list.join(','));
        return;
    }
    if (list.length >= 3) {
        alert('Đã đủ 3 xe so sánh. Gỡ một xe trước khi thêm.');
        return;
    }
    list.push(carId);
    saveCompareList(list);
    window.location.href = '/cars/compare?ids=' + encodeURIComponent(list.join(','));
}

function removeFromCompare(carId) {
    if (carId == null || carId === '') return;
    carId = String(carId);
    const list = getCompareList().filter(function (id) {
        return id !== carId;
    });
    saveCompareList(list);
    if (list.length === 0) {
        window.location.href = '/cars/compare';
    } else {
        window.location.href = '/cars/compare?ids=' + encodeURIComponent(list.join(','));
    }
}

/** Đồng bộ URL ?ids= với localStorage khi mở trang so sánh. */
function syncCompareUrlWithStorage() {
    if (!location.pathname.endsWith('/cars/compare')) return;
    const params = new URLSearchParams(location.search);
    const urlIds = params.get('ids');
    if (urlIds && urlIds.trim() !== '') {
        const fromUrl = urlIds.split(',').map(function (s) {
            return s.trim();
        }).filter(Boolean);
        saveCompareList(fromUrl);
        return;
    }
    const stored = getCompareList();
    if (stored.length > 0) {
        location.replace('/cars/compare?ids=' + encodeURIComponent(stored.join(',')));
    }
}

/**
 * Thông báo nổi khi thêm/gỡ xe yêu thích (Bootstrap Alert, tự đóng sau vài giây).
 * @param {boolean} added true = đã thêm, false = đã gỡ
 */
function showFavoriteActionToast(added) {
    var msg = added
        ? 'Đã thêm xe vào danh sách yêu thích.'
        : 'Đã gỡ xe khỏi danh sách yêu thích.';
    var alertClass = added ? 'alert-success' : 'alert-info';
    var root = document.getElementById('fcarFavToastMount');
    if (!root) {
        root = document.createElement('div');
        root.id = 'fcarFavToastMount';
        document.body.appendChild(root);
    }
    var el = document.createElement('div');
    el.className =
        'alert ' +
        alertClass +
        ' alert-dismissible fade show shadow-sm border-0 position-fixed storefront-fav-toast';
    el.setAttribute('role', 'alert');
    el.innerHTML =
        '<span class="me-2"></span><button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Đóng"></button>';
    el.querySelector('span').textContent = msg;
    root.replaceChildren(el);
    if (window.bootstrap && bootstrap.Alert) {
        setTimeout(function () {
            try {
                bootstrap.Alert.getOrCreateInstance(el).close();
            } catch (e) {
                /* ignore */
            }
        }, 4500);
    }
}

function getCsrfHeaderPair() {
    const headerEl = document.querySelector('meta[name="csrf-header"]');
    const tokenEl = document.querySelector('meta[name="csrf-token"]');
    if (!headerEl || !tokenEl) {
        return null;
    }
    const name = headerEl.getAttribute('content');
    const value = tokenEl.getAttribute('content');
    if (!name || !value) {
        return null;
    }
    return { name: name, value: value };
}

function toggleFavoriteFromList(event, button) {
    if (event) {
        event.preventDefault();
        event.stopPropagation();
    }
    const carId = button.getAttribute('data-car-id');
    if (!carId) {
        return;
    }
    const headers = { Accept: 'application/json', 'Content-Type': 'application/json' };
    const csrf = getCsrfHeaderPair();
    if (csrf) {
        headers[csrf.name] = csrf.value;
    }
    fetch('/api/favorites/toggle/' + encodeURIComponent(carId), {
        method: 'POST',
        credentials: 'same-origin',
        headers: headers,
        body: '{}'
    })
        .then(function (res) {
            if (res.status === 401) {
                const back = encodeURIComponent(window.location.pathname + window.location.search);
                window.location.href = '/auth/login?redirect=' + back;
                return null;
            }
            if (!res.ok) {
                return res.json().then(function (body) {
                    const msg =
                        body && typeof body.error === 'string' ? body.error : 'Không thể cập nhật yêu thích.';
                    throw new Error(msg);
                });
            }
            return res.json();
        })
        .then(function (data) {
            if (!data) {
                return;
            }
            const on = data.favorited === true;
            button.classList.toggle('car-card-store__fav--on', on);
            button.classList.toggle('car-detail-fav-btn--on', on);
            const icon = button.querySelector('i');
            if (icon) {
                const headerFav = button.classList.contains('car-detail-fav-header');
                const detailBtn = button.classList.contains('car-detail-fav-btn');
                if (headerFav) {
                    icon.className = on ? 'bi bi-heart-fill' : 'bi bi-heart';
                } else if (detailBtn) {
                    icon.className = on ? 'bi bi-heart-fill me-1' : 'bi bi-heart me-1';
                } else {
                    icon.className = on ? 'bi bi-heart-fill' : 'bi bi-heart';
                }
            }
            button.title = on ? 'Bỏ yêu thích' : 'Yêu thích';
            button.setAttribute('aria-label', on ? 'Bỏ yêu thích' : 'Thêm vào yêu thích');
            const label = button.querySelector('.car-detail-fav-label');
            if (label) {
                label.textContent = on ? 'Đã yêu thích' : 'Yêu thích';
            }
            showFavoriteActionToast(on);
        })
        .catch(function (err) {
            alert(err.message || 'Không thể cập nhật yêu thích.');
        });
}

function openComparePage() {
    const list = getCompareList();
    if (list.length === 0) {
        window.location.href = '/cars/compare';
        return;
    }
    window.location.href = '/cars/compare?ids=' + encodeURIComponent(list.join(','));
}

/**
 * Checkbox "Chỉ xem các thông số khác nhau": ẩn dòng mà mọi ô dữ liệu giống nhau (so khớp theo nội dung hiển thị).
 * Dòng có data-compare-ignore-diff luôn hiển thị (vd. nút Chi tiết).
 */
function initCompareDiffFilter() {
    const cb = document.getElementById('compareOnlyDiff');
    const table = document.getElementById('compareSpecsTable');
    if (!cb || !table) return;

    function cellTexts(tr) {
        return Array.from(tr.querySelectorAll('td')).map(function (td) {
            return (td.innerText || '').replace(/\s+/g, ' ').trim();
        });
    }

    function markRows() {
        table.querySelectorAll('tbody tr').forEach(function (tr) {
            if (tr.getAttribute('data-compare-ignore-diff') === 'true') {
                tr.dataset.compareSame = '0';
                return;
            }
            const texts = cellTexts(tr);
            if (texts.length < 2) {
                tr.dataset.compareSame = '0';
                return;
            }
            const first = texts[0];
            const allSame = texts.every(function (t) {
                return t === first;
            });
            tr.dataset.compareSame = allSame ? '1' : '0';
        });
    }

    function apply() {
        const on = cb.checked;
        table.querySelectorAll('tbody tr').forEach(function (tr) {
            if (tr.getAttribute('data-compare-ignore-diff') === 'true') {
                tr.classList.remove('d-none');
                return;
            }
            if (!on) {
                tr.classList.remove('d-none');
                return;
            }
            if (tr.dataset.compareSame === '1') {
                tr.classList.add('d-none');
            } else {
                tr.classList.remove('d-none');
            }
        });
    }

    markRows();
    cb.addEventListener('change', apply);
}
