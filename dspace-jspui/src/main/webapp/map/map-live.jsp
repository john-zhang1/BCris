    <div class="panel-body">
      <div id="leafletmap" class="hidden-xs" style="width: 100%; height: 235px;"></div>
    </div>

  <script type="text/javascript">
    // var map = L.map('leafletmap').setView([27.916388,120.653688], 5);

    // L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    //   attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors',
    //   maxZoom: 18,
    //   minZoom: 2,
    //   id: 'lib-zzd.cig7yktpl0489unlx2e5ielz9',
    //   accessToken: 'pk.eyJ1IjoibGliLXp6ZCIsImEiOiJjaWc3eWt2MWEwNDZ6dXprb2Z6dzk5cTJrIn0.MGKAAmkhNF35HHG-yEjh5Q'
    // }).addTo(map);

    // L.marker([27.916388,120.653688]).addTo(map);


    var map = L.map('leafletmap').setView([30.5,95.3], 4);
        var csvData, csvData2;
        var markersLayer;
        var timer;
        var timerInterval = 1 * 1000;
        var previousLayer = '';

        var layer = L.tileLayer('http://{s}.tile.osm.org/{z}/{x}/{y}.png', {
            attribution: '&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors'
        }).addTo(map);

        function Point(lat, lng, val) {
            this.latitude = lat;
            this.longitude = lng;
            this.val = val;
        }

        csvData1 = [];
        csvData2 = [{latitude: 35.01, longitude: 94.5}];
        csvData3 = [{latitude: 36.01, longitude: 90.01}];
        csvData4 = [];
        csvData5 = [{latitude: 38.01, longitude: 85.01}];
        csvData6 = [];
        csvData7 = [{latitude: 35.01, longitude: 94.5}, {latitude: 36.01, longitude: 90.01}];
        csvData8 = [];
        csvData9 = [{latitude: 35.01, longitude: 94.5}, {latitude: 36.01, longitude: 90.01}, {latitude: 38.01, longitude: 85.01}];
        csvData10 = [{latitude: 27.01, longitude: 108.5}, {latitude: 36.01, longitude: 90.01}, {latitude: 38.01, longitude: 85.01}];
        csvData11 = [];
        csvData12 = [{latitude: 27.01, longitude: 108.5}, {latitude: 36.01, longitude: 90.01}];
        csvData13 = [{latitude: 32.01, longitude: 84.5}, {latitude: 36.01, longitude: 90.01}, {latitude: 38.01, longitude: 85.01}];
        csvData14 = [];
        csvData15 = [{latitude: 38.01, longitude: 91.5}, {latitude: 39.01, longitude: 87.01}, {latitude: 41.01, longitude: 82.01}];
        csvData16 = [];
        csvData17 = [{latitude: 40.01, longitude: 90.5}, {latitude: 41.01, longitude: 86.01}, {latitude: 43.01, longitude: 81.01}];

        var csvArray = [csvData1, csvData2, csvData3, csvData4, csvData5, csvData6, csvData7, csvData8, csvData9, csvData10, csvData11, csvData12, csvData13, csvData14, csvData15, csvData16, csvData17];

        function animateMap() {
            timer = setInterval (function() {
                var rate = Math.floor(Math.random() * 18);
                if(previousLayer != ''){
                    map.removeLayer(previousLayer)
                }
                createMarkers(csvArray[rate]);
            }, timerInterval);
        }


        animateMap();

        function step(){
            markersLayer.eachLayer(function(layer) {
                onEachFeature(layer);
            });
        }


        function createMarkers(csvData) {

            var r = 5;

            var markerStyle = {
                radius: r,
                fillColor: "#39F",
            };

            var markersArray = [];

            for (var i=0; i<csvData.length; i++) {
                var feature = {};
                feature.properties = csvData[i];
                var lat = Number(feature.properties.latitude);
                var lng = Number(feature.properties.longitude);
                var marker = L.circleMarker([lat,lng], markerStyle);
                marker.feature = feature;
                markersArray.push(marker);
            };

            //create a markers layer with all of the circle markers
            markersLayer = L.featureGroup(markersArray);
            previousLayer = markersLayer;

            //add the markers layer to the map
            markersLayer.addTo(map);

            // markersLayer.eachLayer(function(layer){
            //     onEachFeature(layer);
            // });
        }

        function onEachFeature(layer) {
            // var area = layer.feature.properties.value * scaleFactor;
            // var radius = Math.sqrt(area/Math.PI);
            // layer.setRadius(radius);
        }


  </script>
