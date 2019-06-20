    <div class="panel-body">
      <div id="leafletmap" class="hidden-xs" style="width: 100%; height: 235px;"></div>
    </div>

  <script type="text/javascript">
    var map = L.map('leafletmap').setView([27.916388,120.653688], 5);

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
      maxZoom: 18,
      minZoom: 2,
      id: 'lib-zzd.cig7yktpl0489unlx2e5ielz9',
      accessToken: 'pk.eyJ1IjoibGliLXp6ZCIsImEiOiJjaWc3eWt2MWEwNDZ6dXprb2Z6dzk5cTJrIn0.MGKAAmkhNF35HHG-yEjh5Q'
    }).addTo(map);

    L.marker([27.916388,120.653688]).addTo(map);

  </script>
