import plotly.plotly as py
import codecs
import json
import plotly
#from plotly.offline import iplot,init_notebook_mode
#import plotly.offline as offline

#offline.init_notebook_mode()

plotly.tools.set_credentials_file(username='yxyang', api_key='jQ4G9UXBUCtAx1qpLYos')

from plotly.graph_objs import *
mapbox_access_token = "s8quitedi4"

f = codecs.open('/Users/yxyang/Documents/CPS512/final_data/node1.planetlab.albany.edu.txt')
cities = []
colors = ["rgb(0,116,217)","rgb(255,65,54)","rgb(133,20,75)","rgb(255,133,27)","lightgrey"]

x = f.readlines()
i=-1
for line in x:
    i=(i+1)%len(colors)
    #print line["d_name"]
    content = eval(line)
    print content
    #print type(content)




    city = dict(
    type = 'scattergeo',
            locationmode = 'USA-states',
            lon = content['longitude'],
            lat = content['latitude'],
            text = content['ip'],
            marker = dict(
                size = 1,
                color = colors[i],
                line = dict(width=0.5, color='rgb(40,40,40)'),
                sizemode = 'area'
            ),
        name = content["d_name"]

    )
    cities.append(city)

layout = dict(
        title = 'IP US city ditributions<br>(Click legend to toggle traces)',
        showlegend = True,



        geo = dict(
            scope='usa',
            projection=dict( type='albers usa' ),
            showland = True,
            landcolor = 'rgb(217, 217, 217)',
            subunitwidth=1,
            countrywidth=1,
            countrycolor="rgb(255, 255, 255)"
        ),
    )

fig = dict( data=cities, layout=layout )
py.iplot(fig, validate=False, filename='d3-bubble-map-populations' )