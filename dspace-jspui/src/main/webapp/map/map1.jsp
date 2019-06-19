    <div class="panel-body">
      <div id="leafletmap" class="hidden-xs" style="width: 100%; height: 235px;"></div>
    </div>

  <script type="text/javascript">
    var map = L.map('leafletmap').setView([27.916388,120.653688], 1);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
    }).addTo(map);

    L.marker([27.916388,120.653688]).addTo(map);

  </script>