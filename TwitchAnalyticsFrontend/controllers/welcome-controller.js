var welcomeController = angular.module('WelcomeControllers', []);

welcomeController.controller('WelcomeController',function($scope, $filter, $timeout, HBaseService) {
  $scope.chatdata=[];
  $scope.viewersdata=[];
  parseHBaseCell = function(data, col){

    data = data.replace("{(","");
    data = data.replace(")}","");

    data = data.split("),(")

    data.data=[]
    data.forEach(function(cell){
      cell = cell.split(",");
      if(col.indexOf("viewerspermin") >=0){
        cell[0] = cell[0];
        cell[1] = parseFloat(cell[1])

        cell = {timestamp: cell[0], viewers: cell[1]}
      }
      else if(col.indexOf("chatspermin") >=0){
        if(cell[1] != undefined){
          cell[1] = Number(cell[1]);
          cell[2] = Number(cell[2]);
          cell[3] = Number(cell[3]);

          cell = {timestamp: cell[0], positive: cell[1], negative: cell[2], neutral: cell[3]}
        }
      }
      else if(col.indexOf("metadata") >=0){
        cell[4] = cell[4]==="false" ? false : true;
        if(Object.prototype.toString.call(new Date(cell[6])) === "[object Date]"){
          if ( isNaN( new Date(cell[6]).getTime() ) ) {  // d.valueOf() could also work
            cell[5] = parseFloat(cell[6]);
            cell[6] = new Date(cell[7]);
          }else{
            cell[5] = parseFloat(cell[5]);
            cell[6] = new Date(cell[6]);
          }
        }


        cell = {game: cell[2], status: cell[3], mature: cell[4], fps: cell[5], timestamp: cell[6]}
      }
      data.data.push(cell);
    });
    data=data.data;

    return data;
  }
  $scope.stream="rocketbeanstv";
  $scope.date = new Date();
  $scope.loadData = function(){
    $scope.chatdata=[];
    $scope.viewersdata=[];
    if($scope.chatperminapi != undefined)
      $scope.chatperminapi.refresh();
    if($scope.viewersperminapi != undefined)
      $scope.viewersperminapi.refresh();
    $scope.loading = true;
    HBaseService.queryHBase("twitchdata",$scope.stream.toLowerCase() + $filter('date')($scope.date, "yyyyMMdd") + "*").then(function(result){
      $scope.hbaseres = result.data.Row;
      $scope.hbaseres.forEach(function(row) {
          row.key=atob(row.key);
          row.Cell.forEach(function(cell){
            cell.column = atob(cell.column);
            cell.$ = parseHBaseCell(atob(cell.$), cell.column);
            cell.timestamp = new Date(cell.timestamp);
          });
      });
      $scope.positivdata = [];
      $scope.negativdata = [];
      $scope.neutraldata = [];
      $scope.viewerspermindata = [];
      $scope.hbaseres.forEach(function(row){
        row.Cell.forEach(function(cell){
          if(cell.column.indexOf("chatspermin") > -1){
            cell.$.forEach(function(chat){
              if(chat.timestamp != undefined){
                time = new Date(chat.timestamp.substr(0,4)+"-"+chat.timestamp.substr(4,2)+"-"+chat.timestamp.substr(6,2)+"T"+chat.timestamp.substr(8,2)+":"+chat.timestamp.substr(10,2)+":00").getTime();
                $scope.positivdata.push([time,chat.positive])
                $scope.negativdata.push([time,chat.negative])
                $scope.neutraldata.push([time,chat.neutral])
              }
            });
          }
          if(cell.column.indexOf("viewerspermin") > -1){
            cell.$.forEach(function(chat){
              time = new Date(chat.timestamp.substr(0,4)+"-"+chat.timestamp.substr(4,2)+"-"+chat.timestamp.substr(6,2)+"T"+chat.timestamp.substr(8,2)+":"+chat.timestamp.substr(10,2)+":00+0100").getTime();
              $scope.viewerspermindata.push([time,chat.viewers]);
            });
          }
        });
      });

      $scope.positivdata.sort(function(a, b){
          var keyA = new Date(a[0]),
              keyB = new Date(b[0]);
          // Compare the 2 dates
          if(keyA < keyB) return -1;
          if(keyA > keyB) return 1;
          return 0;
      });
      $scope.neutraldata.sort(function(a, b){
          var keyA = new Date(a[0]),
              keyB = new Date(b[0]);
          // Compare the 2 dates
          if(keyA < keyB) return -1;
          if(keyA > keyB) return 1;
          return 0;
      });
      $scope.negativdata.sort(function(a, b){
          var keyA = new Date(a[0]),
              keyB = new Date(b[0]);
          // Compare the 2 dates
          if(keyA < keyB) return -1;
          if(keyA > keyB) return 1;
          return 0;
      });
      $scope.viewerspermindata.sort(function(a, b){
          var keyA = new Date(a[0]),
              keyB = new Date(b[0]);
          // Compare the 2 dates
          if(keyA < keyB) return -1;
          if(keyA > keyB) return 1;
          return 0;
      });
      $scope.loading = false;


      $timeout(function() {
        $scope.chatdata=[];
        $scope.viewersdata=[];
        if($scope.positivdata.length > 0 || $scope.negativdata.length > 0 || $scope.neutraldata.length > 0){
          $scope.chatdata.push({
            key: "Positiv",
            values:  $scope.positivdata
          });
          $scope.chatdata.push({
            key: "Neutral",
            values:  $scope.neutraldata
          });
          $scope.chatdata.push({
            key: "Negativ",
            values:  $scope.negativdata
          });
        }

        if($scope.viewerspermindata.length > 0){
          $scope.viewersdata.push({
            key: "Viewers",
            values: $scope.viewerspermindata
          });
        }
        $scope.chatperminapi.refresh();
        $scope.viewersperminapi.refresh();
    }, 200);
    });
  }
  $scope.loadData();


  $scope.chatoptions = {
            chart: {
                type: 'stackedAreaChart',
                height: 350,
                margin : {
                    top: 20,
                    right: 100,
                    bottom: 30,
                    left: 100
                },
                x: function(d){return d[0];},
                y: function(d){return d[1];},
                useVoronoi: false,
                clipEdge: true,
                duration: 100,
                color: ['LIMEGREEN', 'GOLD', 'FIREBRICK'],
                useInteractiveGuideline: true,
                xAxis: {
                    showMaxMin: false,
                    tickFormat: function(d) {
                        return $filter('date')(d, "HH:mm");
                    }
                },
                yAxis: {
                    tickFormat: function(d){
                        return d3.format(',.0f')(d);
                    }
                },
                zoom: {
                    enabled: true,
                    scaleExtent: [1, 10],
                    useFixedDomain: false,
                    useNiceScale: false,
                    horizontalOff: false,
                    verticalOff: false,
                    unzoomEventType: 'dblclick.zoom'
                }
            }
        };

       $scope.viewersoptions = {
                 chart: {
                     type: 'stackedAreaChart',
                     height: 350,
                     margin : {
                         top: 20,
                         right: 100,
                         bottom: 30,
                         left: 100
                     },
                     x: function(d){return d[0];},
                     y: function(d){return d[1];},
                     useVoronoi: false,
                     clipEdge: true,
                     duration: 100,
                     color: ['STEELBLUE'],
                     useInteractiveGuideline: true,
                     xAxis: {
                         showMaxMin: false,
                         tickFormat: function(d) {
                             return $filter('date')(d, "HH:mm");
                         }
                     },
                     yAxis: {
                         tickFormat: function(d){
                             return d3.format(',.0f')(d);
                         }
                     },
                     interactiveLayer:{
                       tooltip:{
                         contentGenerator: function(data) {
                           time = data.value;
                           value = data.series[0].stackedValue.y
                           var metadata;
                           $scope.hbaseres.forEach(function(row){
                             row.Cell.forEach(function(data){
                               if(data.column.indexOf("metadata")>-1){
                                 data.$.forEach(function(value){
                                   if($filter('date')(value.timestamp,'HH:mm') === time){
                                     metadata = value;
                                   }
                                 })
                               }
                             })
                           })
                           tooltip = value+' Zuschauer um '+ time;
                           if(metadata != undefined){
                             tooltip += '<br>'+ "Game: " + metadata.game + "<br>Status: " + metadata.status + "<br>FPS: " + metadata.fps
                           }else{
                             tooltip += '<br> Leider keine weiteren Informationen verfügbar'
                           }
                          return  tooltip ;
                      }
                    }
                  },
                     zoom: {
                         enabled: true,
                         scaleExtent: [1, 10],
                         useFixedDomain: false,
                         useNiceScale: false,
                         horizontalOff: false,
                         verticalOff: false,
                         unzoomEventType: 'dblclick.zoom'
                     }
                 }
             };

});

welcomeController.controller('HeaderController',function($scope) {
});

welcomeController.controller('PredictionController',function($scope, $filter, PredictionService) {
  $scope.channel = "rocketbeanstv";
  $scope.game="Gaming Talk Show";
  $scope.duration = 60;
  $scope.daytime = "Abends";
  $scope.frames = 60;
  $scope.streamingdate = new Date();
  $scope.streamingdate.setDate($scope.streamingdate.getDate()+1);
  $scope.predict = function(){
    $scope.loading = true;
    $scope.channel = $scope.channel.replace(" ","_").replace(",","_")
    $scope.game = $scope.game.replace(" ","_").replace(",","_")
    PredictionService.predict($scope.channel, $scope.game, $scope.duration, $scope.daytime, $scope.frames, $filter('date')($scope.streamingdate,"yyyy-MM-dd HH:mm:ss")).then(function(result){
      $scope.predictionclass = result.data;
      $scope.predictionResult = ($scope.predictionclass*1000) + " bis " + (($scope.predictionclass + 1) * 1000);
      $scope.loading=false;
    });

  }
});
