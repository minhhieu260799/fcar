document.addEventListener('DOMContentLoaded', function () {
  const galleryContainer = document.getElementById('galleryImagesContainer');
  const btnAddGalleryImage = document.getElementById('btnAddGalleryImage');
  const colorsContainer = document.getElementById('colorsContainer');
  const btnAddColor = document.getElementById('btnAddColor');
  const attributesContainer = document.getElementById('attributesContainer');
  const btnAddAttribute = document.getElementById('btnAddAttribute');
  const brandSelect = document.getElementById('addDefinitionBrand');
  const modelSelect = document.getElementById('addDefinitionModel');
  const segmentSelect = document.getElementById('addDefinitionSegment');
  const yearSelect = document.getElementById('addDefinitionYear');
  const bodyTypeSelect = document.getElementById('addDefinitionBodyType');
  const addModal = document.getElementById('addCarDefinitionModal');

  // ----- Khi mở modal: chọn "Chọn ..." cho tất cả dropdown -----
  if (addModal) {
    addModal.addEventListener('show.bs.modal', function () {
      if (brandSelect) brandSelect.selectedIndex = 0;
      if (modelSelect) modelSelect.selectedIndex = 0;
      if (segmentSelect) segmentSelect.selectedIndex = 0;
      if (yearSelect) yearSelect.selectedIndex = 0;
      if (bodyTypeSelect) bodyTypeSelect.selectedIndex = 0;
      if (brandSelect && modelSelect) filterModelsByBrand();
      if (modelSelect && segmentSelect) filterSegmentsByModel();
    });
  }

  // ----- Dòng xe theo Hãng -----
  function filterModelsByBrand() {
    if (!brandSelect || !modelSelect) return;
    var brandId = brandSelect.value;
    var options = modelSelect.querySelectorAll('option');
    var firstVisible = null;
    for (var i = 0; i < options.length; i++) {
      var opt = options[i];
      var optBrandId = opt.getAttribute('data-brand-id');
      if (optBrandId === null || opt.value === '') {
        opt.hidden = false;
        continue;
      }
      var visible = optBrandId === brandId;
      opt.hidden = !visible;
      if (visible && firstVisible === null) firstVisible = opt;
    }
    var selected = modelSelect.options[modelSelect.selectedIndex];
    if (selected && selected.hidden) {
      modelSelect.selectedIndex = 0;
    }
    if (modelSelect && segmentSelect) filterSegmentsByModel();
  }

  // ----- Phiên bản theo Dòng xe -----
  function filterSegmentsByModel() {
    if (!modelSelect || !segmentSelect) return;
    var modelId = modelSelect.value;
    var options = segmentSelect.querySelectorAll('option');
    var firstVisible = null;
    for (var i = 0; i < options.length; i++) {
      var opt = options[i];
      var optModelId = opt.getAttribute('data-model-id');
      if (optModelId === null || opt.value === '') {
        opt.hidden = false;
        continue;
      }
      var visible = optModelId === modelId;
      opt.hidden = !visible;
      if (visible && firstVisible === null) firstVisible = opt;
    }
    var selected = segmentSelect.options[segmentSelect.selectedIndex];
    if (selected && selected.hidden) {
      segmentSelect.selectedIndex = 0;
    }
  }

  if (brandSelect && modelSelect) {
    brandSelect.addEventListener('change', filterModelsByBrand);
    filterModelsByBrand();
  }
  if (modelSelect && segmentSelect) {
    modelSelect.addEventListener('change', filterSegmentsByModel);
    filterSegmentsByModel();
  }

  // ----- Bộ ảnh -----
  if (galleryContainer && btnAddGalleryImage) {
    btnAddGalleryImage.addEventListener('click', function () {
      const first = galleryContainer.querySelector('.gallery-item');
      if (!first) {
        return;
      }
      const clone = first.cloneNode(true);
      const input = clone.querySelector('input[type="file"]');
      if (input) {
        input.value = '';
      }
      galleryContainer.appendChild(clone);
    });

    galleryContainer.addEventListener('click', function (e) {
      const btn = e.target.closest('[data-role="remove-gallery"]');
      if (!btn) return;
      const items = galleryContainer.querySelectorAll('.gallery-item');
      if (items.length <= 1) {
        // Ít nhất phải còn 1 ảnh
        return;
      }
      const item = btn.closest('.gallery-item');
      if (item) {
        item.remove();
      }
    });
  }

  // ----- Màu sắc -----
  if (colorsContainer && btnAddColor) {
    btnAddColor.addEventListener('click', function () {
      const first = colorsContainer.querySelector('.color-item');
      if (!first) {
        return;
      }
      const clone = first.cloneNode(true);
      const input = clone.querySelector('input[type="color"]');
      if (input) {
        input.value = '#ffffff';
      }
      colorsContainer.appendChild(clone);
    });

    colorsContainer.addEventListener('click', function (e) {
      const btn = e.target.closest('[data-role="remove-color"]');
      if (!btn) return;
      const items = colorsContainer.querySelectorAll('.color-item');
      if (items.length <= 1) {
        // Ít nhất phải còn 1 màu
        return;
      }
      const item = btn.closest('.color-item');
      if (item) {
        item.remove();
      }
    });
  }

  // ----- Thông số khác -----
  if (attributesContainer && btnAddAttribute) {
    btnAddAttribute.addEventListener('click', function () {
      const wrapper = document.createElement('div');
      wrapper.className = 'attr-item';
      wrapper.style.maxWidth = 'min(100%, 22rem)';
      wrapper.innerHTML = `
        <div class="mb-1">
          <input type="text"
                 class="form-control form-control-sm"
                 name="extraAttributeNames"
                 placeholder="Tên thông số"
                 maxlength="50">
        </div>
        <div class="d-flex align-items-center">
          <input type="text"
                 class="form-control form-control-sm flex-grow-1"
                 name="extraAttributeValues"
                 placeholder="Giá trị thông số"
                 maxlength="100">
          <button type="button"
                  class="btn btn-outline-danger btn-sm ms-1"
                  data-role="remove-attr">&times;</button>
        </div>
      `;
      attributesContainer.appendChild(wrapper);
    });

    attributesContainer.addEventListener('click', function (e) {
      const btn = e.target.closest('[data-role="remove-attr"]');
      if (!btn) return;
      const item = btn.closest('.attr-item');
      if (item) {
        item.remove();
      }
    });
  }

  // ----- Cập nhật mẫu xe (modal) -----
  var editModal = document.getElementById('editCarDefinitionModal');
  var editForm = document.getElementById('editDefinitionForm');
  var editNewColorsContainer = document.getElementById('editNewColorsContainer');
  var editBtnAddColor = document.getElementById('editBtnAddColor');
  var editAttributesContainer = document.getElementById('editAttributesContainer');
  var editBtnAddAttribute = document.getElementById('editBtnAddAttribute');
  var editGalleryContainer = document.getElementById('editGalleryContainer');
  var editBtnAddGallery = document.getElementById('editBtnAddGallery');

  function openEditModal(id) {
    if (!editForm || !editModal) return;
    editForm.action = '/admin/cars/definitions/' + id;
    fetch('/admin/cars/definitions/' + id + '/edit-data')
      .then(function (r) { return r.json(); })
      .then(function (data) {
        document.getElementById('editBrandId').value = data.brandId || '';
        document.getElementById('editModelId').value = data.modelId || '';
        document.getElementById('editSegmentId').value = data.segmentId || '';
        document.getElementById('editProductionYear').value = data.productionYear || '';
        document.getElementById('editBrandName').value = data.brandName || '';
        document.getElementById('editModelName').value = data.modelName || '';
        document.getElementById('editSegmentName').value = data.segmentName || '';
        document.getElementById('editProductionYearDisplay').value = data.productionYear || '';
        document.getElementById('editFuelType').value = data.fuelType || '';
        document.getElementById('editSeats').value = data.seats != null ? data.seats : '';
        document.getElementById('editHorsepower').value = data.horsepower != null ? data.horsepower : '';
        document.getElementById('editDescription').value = data.description || '';
        document.getElementById('editSalePrice').value = data.salePrice != null ? data.salePrice : '';
        document.getElementById('editPromoPrice').value = data.promoPrice != null ? data.promoPrice : '';

        var editBodyTypeSelect = document.getElementById('editBodyType');
        if (editBodyTypeSelect) {
          while (editBodyTypeSelect.options.length > 1) editBodyTypeSelect.remove(1);
          if (data.bodyTypes && data.bodyTypes.length) {
            data.bodyTypes.forEach(function (bt) {
              var opt = document.createElement('option');
              opt.value = bt.name || '';
              opt.textContent = bt.displayName || bt.name || '';
              editBodyTypeSelect.appendChild(opt);
            });
          }
          editBodyTypeSelect.value = data.bodyType || '';
        }

        var existingImagesList = document.getElementById('editExistingImagesList');
        var existingImagesEmpty = document.getElementById('editExistingImagesEmpty');
        if (existingImagesList) {
          existingImagesList.innerHTML = '';
          editForm.querySelectorAll('input[name="imageIdsToDelete"]').forEach(function (el) { el.remove(); });
          if (data.existingImages && data.existingImages.length) {
            if (existingImagesEmpty) existingImagesEmpty.classList.add('d-none');
            data.existingImages.forEach(function (img) {
              var wrap = document.createElement('div');
              wrap.className = 'edit-existing-image-item position-relative border rounded overflow-hidden';
              wrap.style.width = '100px';
              wrap.style.height = '80px';
              var imgEl = document.createElement('img');
              imgEl.src = img.imageUrl || '';
              imgEl.alt = '';
              imgEl.style.width = '100%';
              imgEl.style.height = '100%';
              imgEl.style.objectFit = 'cover';
              wrap.appendChild(imgEl);
              var badge = document.createElement('span');
              badge.className = 'position-absolute top-0 start-0 badge bg-secondary';
              badge.textContent = img.cover ? 'Đại diện' : 'Gallery';
              wrap.appendChild(badge);
              var btn = document.createElement('button');
              btn.type = 'button';
              btn.className = 'position-absolute top-0 end-0 btn btn-sm btn-danger py-0 px-1';
              btn.textContent = '×';
              btn.title = 'Xóa ảnh';
              btn.setAttribute('data-image-id', img.id);
              wrap.appendChild(btn);
              existingImagesList.appendChild(wrap);
            });
            existingImagesList.addEventListener('click', function (e) {
              var btn = e.target.closest('button[data-image-id]');
              if (!btn) return;
              var id = btn.getAttribute('data-image-id');
              if (!id) return;
              var hid = document.createElement('input');
              hid.type = 'hidden';
              hid.name = 'imageIdsToDelete';
              hid.value = id;
              editForm.appendChild(hid);
              btn.closest('.edit-existing-image-item').remove();
            });
          } else {
            if (existingImagesEmpty) existingImagesEmpty.classList.remove('d-none');
          }
        }

        var existingColorsEl = document.getElementById('editExistingColors');
        existingColorsEl.innerHTML = '';
        editForm.querySelectorAll('input[name="colorIdsToDelete"]').forEach(function (el) { el.remove(); });
        if (data.existingColors && data.existingColors.length) {
          data.existingColors.forEach(function (item) {
            var colorId = item.id;
            var colorValue = (item.colorValue != null ? item.colorValue : '').trim();
            var wrap = document.createElement('span');
            wrap.className = 'edit-existing-color-item d-inline-flex align-items-center gap-1 border rounded px-2 py-1';
            wrap.setAttribute('data-color-id', colorId);
            wrap.setAttribute('data-color-value', colorValue);
            if (colorValue.indexOf('#') === 0) {
              var swatch = document.createElement('span');
              swatch.style.cssText = 'width:1.2rem;height:1.2rem;border-radius:4px;background:' + colorValue;
              wrap.appendChild(swatch);
            }
            wrap.appendChild(document.createTextNode(colorValue));
            var btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'btn btn-link btn-sm p-0 text-danger ms-1';
            btn.textContent = '×';
            btn.title = 'Xóa màu';
            wrap.appendChild(btn);
            existingColorsEl.appendChild(wrap);
          });
        } else {
          existingColorsEl.appendChild(document.createTextNode('Chưa có màu nào.'));
        }

        editNewColorsContainer.innerHTML = '';
        editAttributesContainer.innerHTML = '';
        if (data.extraAttributes && data.extraAttributes.length) {
          data.extraAttributes.forEach(function (p) {
            addEditAttributeRow(p.name || '', p.value || '');
          });
        }

        var firstGallery = editGalleryContainer.querySelector('.edit-gallery-item');
        while (editGalleryContainer.children.length > 1) {
          editGalleryContainer.removeChild(editGalleryContainer.lastChild);
        }
        if (firstGallery) {
          var input = firstGallery.querySelector('input[type="file"]');
          if (input) input.value = '';
        }

        var coverInput = editForm.querySelector('input[name="coverImage"]');
        if (coverInput) coverInput.value = '';

        editForm.setAttribute('data-existing-colors', JSON.stringify(data.existingColors || []));

        var errEl = document.getElementById('editDefinitionError');
        if (errEl) { errEl.classList.add('d-none'); errEl.textContent = ''; }

        new bootstrap.Modal(editModal).show();
      })
      .catch(function () {
        alert('Không tải được dữ liệu mẫu xe.');
      });
  }

  function addEditColorRow() {
    if (!editNewColorsContainer) return;
    var div = document.createElement('div');
    div.className = 'edit-new-color-row d-flex align-items-center';
    div.innerHTML = '<input type="color" class="form-control form-control-color p-0 me-1" name="newColors" value="#ffffff" style="width:2.5rem;height:2rem">' +
      '<button type="button" class="btn btn-outline-danger btn-sm" data-role="remove-edit-color">&times;</button>';
    editNewColorsContainer.appendChild(div);
  }

  function addEditAttributeRow(name, value) {
    if (!editAttributesContainer) return;
    var wrapper = document.createElement('div');
    wrapper.className = 'attr-item';
    wrapper.style.maxWidth = 'min(100%, 22rem)';
    wrapper.innerHTML = '<div class="mb-1"><input type="text" class="form-control form-control-sm" name="extraAttributeNames" placeholder="Tên thông số" maxlength="50"></div>' +
      '<div class="d-flex align-items-center"><input type="text" class="form-control form-control-sm flex-grow-1" name="extraAttributeValues" placeholder="Giá trị thông số" maxlength="100">' +
      '<button type="button" class="btn btn-outline-danger btn-sm ms-1" data-role="remove-edit-attr">&times;</button></div>';
    editAttributesContainer.appendChild(wrapper);
    var inputs = wrapper.querySelectorAll('input');
    if (inputs[0]) inputs[0].value = name || '';
    if (inputs[1]) inputs[1].value = value || '';
  }

  if (editBtnAddColor) {
    editBtnAddColor.addEventListener('click', addEditColorRow);
  }
  if (editNewColorsContainer) {
    editNewColorsContainer.addEventListener('click', function (e) {
      if (e.target.closest('[data-role="remove-edit-color"]')) {
        e.target.closest('.edit-new-color-row').remove();
      }
    });
  }
  if (editBtnAddAttribute) {
    editBtnAddAttribute.addEventListener('click', function () {
      addEditAttributeRow('', '');
    });
  }
  if (editAttributesContainer) {
    editAttributesContainer.addEventListener('click', function (e) {
      if (e.target.closest('[data-role="remove-edit-attr"]')) {
        e.target.closest('.attr-item').remove();
      }
    });
  }
  if (editBtnAddGallery && editGalleryContainer) {
    editBtnAddGallery.addEventListener('click', function () {
      var first = editGalleryContainer.querySelector('.edit-gallery-item');
      if (!first) return;
      var clone = first.cloneNode(true);
      var input = clone.querySelector('input[type="file"]');
      if (input) input.value = '';
      editGalleryContainer.appendChild(clone);
    });
    editGalleryContainer.addEventListener('click', function (e) {
      if (!e.target.closest('[data-role="remove-edit-gallery"]')) return;
      var items = editGalleryContainer.querySelectorAll('.edit-gallery-item');
      if (items.length <= 1) return;
      e.target.closest('.edit-gallery-item').remove();
    });
  }

  document.querySelectorAll('.edit-definition-btn').forEach(function (btn) {
    btn.addEventListener('click', function () {
      var id = this.getAttribute('data-id');
      if (id) openEditModal(id);
    });
  });

  // ----- Xóa màu đã có (modal cập nhật): chỉ cho xóa khi còn hơn 1 màu -----
  var editExistingColorsEl = document.getElementById('editExistingColors');
  if (editExistingColorsEl && editForm) {
    editExistingColorsEl.addEventListener('click', function (e) {
      var btn = e.target.closest('.edit-existing-color-item button');
      if (!btn) return;
      var item = btn.closest('.edit-existing-color-item');
      if (!item) return;
      var items = editExistingColorsEl.querySelectorAll('.edit-existing-color-item');
      if (items.length <= 1) return;
      var colorId = item.getAttribute('data-color-id');
      if (colorId) {
        var hid = document.createElement('input');
        hid.type = 'hidden';
        hid.name = 'colorIdsToDelete';
        hid.value = colorId;
        editForm.appendChild(hid);
      }
      item.remove();
    });
  }

  // ----- Kiểm tra màu trùng: Thêm mẫu xe mới (bubble ngay trên input color) -----
  var addForm = document.querySelector('#addCarDefinitionModal form');
  if (addForm) {
    addForm.addEventListener('submit', function (e) {
      var inputs = addForm.querySelectorAll('input[name="colors"]');
      var seen = {};
      for (var i = 0; i < inputs.length; i++) {
        (function (input) {
          input.addEventListener('input', function () {
            input.setCustomValidity('');
          });
        })(inputs[i]);
        var val = (inputs[i].value || '').trim().toLowerCase();
        if (!val) continue;
        if (seen[val]) {
          e.preventDefault();
          inputs[i].setCustomValidity('Màu sắc không được trùng');
          inputs[i].reportValidity();
          return;
        }
        seen[val] = true;
      }
    });
  }

  // ----- Submit cập nhật mẫu xe qua AJAX, hiển thị lỗi trong modal -----
  if (editForm) {
    editForm.addEventListener('submit', function (e) {
      e.preventDefault();

      var errEl = document.getElementById('editDefinitionError');
      if (errEl) { errEl.classList.add('d-none'); errEl.textContent = ''; }

      // Kiểm tra màu trùng (client): so với màu đã có (còn hiển thị trong modal)
      var existingSet = {};
      var remainingColorItems = document.querySelectorAll('#editExistingColors .edit-existing-color-item');
      remainingColorItems.forEach(function (el) {
        var v = (el.getAttribute('data-color-value') || '').trim().toLowerCase();
        if (v) existingSet[v] = true;
      });
      var newInputs = editForm.querySelectorAll('input[name="newColors"]');
      newInputs.forEach(function (inp) {
        inp.addEventListener('input', function () {
          inp.setCustomValidity('');
        });
      });
      for (var j = 0; j < newInputs.length; j++) {
        var val = (newInputs[j].value || '').trim().toLowerCase();
        if (!val) continue;
        if (existingSet[val]) {
          newInputs[j].setCustomValidity('Màu sắc không được trùng');
          newInputs[j].reportValidity();
          return;
        }
        existingSet[val] = true;
      }

      if (!confirm('Bạn có chắc rằng muốn sửa thông tin mẫu xe?')) return;

      var formData = new FormData(editForm);
      fetch(editForm.action, {
        method: 'POST',
        body: formData,
        headers: { 'X-Requested-With': 'XMLHttpRequest' }
      })
        .then(function (res) {
          return res.json().then(function (data) {
            if (res.ok && data.success) {
              if (data.redirectUrl) {
                window.location.href = data.redirectUrl;
              } else {
                window.location.href = '/admin/cars/definitions?success=updated';
              }
              return;
            }
            if (data.error && errEl) {
              errEl.textContent = data.error;
              errEl.classList.remove('d-none');
            }
          });
        })
        .catch(function () {
          if (errEl) {
            errEl.textContent = 'Có lỗi xảy ra, vui lòng thử lại.';
            errEl.classList.remove('d-none');
          }
        });
    });
  }
});

