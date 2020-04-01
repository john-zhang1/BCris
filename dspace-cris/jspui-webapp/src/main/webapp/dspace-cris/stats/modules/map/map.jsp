<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    https://github.com/CILEA/dspace-cris/wiki/License

--%>
<%--  Leaflet maps  --%>
<c:set var="pieType">location</c:set>
<c:set var="pieType2">city</c:set>
<c:set var="pieType3">countryCode</c:set>


<c:set var="targetDiv" scope="page">div_${data.jspKey}_${statType}_${objectName}_${pieType}</c:set>

<c:set var="jsDataObjectName" scope="page"> data_${statType}_${objectName}_${pieType}_${pieType}</c:set>
<c:set var="jsCityDataObjectName" scope="page"> data_${statType}_${objectName}_${pieType2}_${pieType2}</c:set>
<c:set var="jsCountryCodeDataObjectName" scope="page"> data_${statType}_${objectName}_${pieType3}_${pieType3}</c:set>
<c:if test="${fn:length(data.resultBean.dataBeans[statType][objectName][pieType].dataTable) gt 0}">
  <div class="panel panel-default">
    <div class="panel-heading">
      <h6 class="panel-title"><i class="fa fa-map-marker"></i>
        <fmt:message key="view.stats.map.title" />
      </h6>
    </div>
    <div class="panel-body">
      <div id="${targetDiv}" style="width: 100%; height: 300px;"></div>
    </div>
  </div>

  <script type="text/javascript">

    var dotIconPath = "<%= request.getContextPath()%>/static/css/leaflet/images/doticon.png";
    var ${ jsDataObjectName } = new Array(${ fn: length(data.resultBean.dataBeans[statType][objectName][pieType].dataTable) });
    var ${ jsCityDataObjectName } = new Array(${ fn: length(data.resultBean.dataBeans[statType][objectName][pieType2].dataTable) });
    var ${ jsCountryCodeDataObjectName } = new Array(${ fn: length(data.resultBean.dataBeans[statType][objectName][pieType3].dataTable) });
    <c:forEach items="${data.resultBean.dataBeans[statType][objectName][pieType].dataTable}" var="row" varStatus="status">
      ${jsDataObjectName}[${status.count - 1}]= ['<c:out value="${row.latitude}"/>','<c:out value="${row.longitude}"/>','<c:out value="${row.value}"/>',<c:out value="${row.percentage}"/>];
    </c:forEach >

    <c:forEach items="${data.resultBean.dataBeans[statType][objectName][pieType2].dataTable}" var="row" varStatus="status2">
      ${jsCityDataObjectName}[${status2.count - 1}]= ['<c:out value="${row.label}"/>','<c:out value="${row.value}"/>',<c:out value="${row.percentage}"/>];
    </c:forEach >

    <c:forEach items="${data.resultBean.dataBeans[statType][objectName][pieType3].dataTable}" var="row" varStatus="status3">
      ${jsCountryCodeDataObjectName}[${status3.count - 1}]= ['<c:out value="${row.label}"/>','<c:out value="${row.value}"/>',<c:out value="${row.percentage}"/>];
    </c:forEach >

    var dotIcon = new L.icon({
        iconUrl: dotIconPath,
        iconSize: [10, 10]
    });

    function initialize_${ jsDataObjectName }() {
      if (${ jsDataObjectName }.length == 0) return;
      var leafletmap = L.map("${targetDiv}").setView([${ jsDataObjectName }[0][0], ${ jsDataObjectName }[0][1]], 4);

      L.tileLayer('https://{s}.basemaps.cartocdn.com/light_nolabels/{z}/{x}/{y}{r}.png?access_token={accessToken}', {
        attribution: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, ' +
          '<a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, ' +
          'Imagery &copy; <a href="http://mapbox.com">Mapbox</a>',
        maxZoom: 18,
        minZoom: 2,
        id: 'lib-zzd.cig7yktpl0489unlx2e5ielz9',
        accessToken: 'pk.eyJ1IjoibGliLXp6ZCIsImEiOiJjaWc3eWt2MWEwNDZ6dXprb2Z6dzk5cTJrIn0.MGKAAmkhNF35HHG-yEjh5Q'
      }).addTo(leafletmap);

      for (var i = 0; i < ${ jsDataObjectName }.length; i++) {
        if(${jsCityDataObjectName }[i][0] != "Unknown") {
          marker = new L.marker([${ jsDataObjectName }[i][0], ${ jsDataObjectName }[i][1]],
          {
            icon: dotIcon,
          }).addTo(leafletmap)
          .bindPopup(${jsCityDataObjectName }[i][0] + ', ' + ${jsCountryCodeDataObjectName }[i][0] );
        }
        console.log(${jsDataObjectName});
        console.log(${jsCityDataObjectName});
        console.log(${jsCountryCodeDataObjectName});
      }
  }

  initialize_${ jsDataObjectName }();

  </script>
</c:if>