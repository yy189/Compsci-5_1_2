# -*- coding: utf-8 -*-

import pexpect
import threading
import codecs
import json
import sys
import time
import urllib2
import httplib2
import os
import datetime
import ast

h = httplib2.Http(disable_ssl_certificate_validation=True, timeout=0.1)
AUTO_RECONNECT_TIMES = 5


def parse_IP(ip):
    url = "http://www.freegeoip.net/json/" + ip

    success = 0
    retry = 0
    while (success == 0):
        try:
            resp, content = h.request(url, "GET")
            # print username, resp['status']
            success = 1
            if (resp['status'] != '200'):
                return -1

        except:
            time.sleep(3)
            retry += 1
            # print '[rerty]'
            if (retry == AUTO_RECONNECT_TIMES):
                return -2
    #print content
    return content

def isIPaddr(str1):
    if len(str1.split('.')) == 4:
        return True
    else:
        return False

def main():
    #readfile
    os.chdir('/Users/yxyang/Documents/CPS512/final_project/')
    root = '/Users/yxyang/Documents/CPS512/final_project/'
    listx = ['planetlab02.cs.washington.edu', 'planetlab-5.eecs.cwru.edu', 'planetlab4.mini.pw.edu.pl', 'planetlab4.postel.org',
     'node1.planetlab.albany.edu', 'planetlab1.pop-pa.rnp.br', 'planetlab5.eecs.umich.edu', 'plink.cs.uwaterloo.ca',
     'planetlab5.williams.edu', 'planetlab1.cs.purdue.edu', 'planetlab1.postel.org', 'planetlab-n2.wand.net.nz',
     'planetlab2.pop-pa.rnp.br', 'planetlab-2.calpoly-netlab.net', 'planetlab1.cesnet.cz',
     'whitefall.planetlab.cs.umd.edu', 'planetlab3.williams.edu', 'planetlab2.dtc.umn.edu', 'pl1.eng.monash.edu.au',
     'planetlab1.cs.ubc.ca', 'pl2.ucs.indiana.edu', 'pl1-higashi.ics.es.osaka-u.ac.jp', 'planetlab1.utdallas.edu',
     'lefthand.eecs.harvard.edu', 'planetlabone.ccs.neu.edu', 'pl1.ucs.indiana.edu', 'pl2.eng.monash.edu.au',
     'planetlab3.rutgers.edu', 'planetlab04.cs.washington.edu', 'planetlab3.cs.uoregon.edu',
     'cs-planetlab3.cs.surrey.sfu.ca', 'planetlab4.cs.uoregon.edu', 'planetlab1.pop-mg.rnp.br', 'pl1.rcc.uottawa.ca',
     'planetlab3.eecs.umich.edu', 'planetlab4.williams.edu', 'planetlab2.emich.edu', 'planetlab2.pop-mg.rnp.br',
     'node1.planetlab.mathcs.emory.edu', 'planetlab5.ie.cuhk.edu.hk', 'planetlab1.koganei.itrc.net',
     'planetlab2.koganei.itrc.net', 'planetlab3.comp.nus.edu.sg', 'planetlab1.unr.edu', 'planetlab01.cs.washington.edu',
     'planet-lab4.uba.ar', 'planetlab2.inf.ethz.ch', 'planetlab2.utdallas.edu', 'planetlab2.cs.purdue.edu',
     'planetlab1.comp.nus.edu.sg', 'planetlab2.cesnet.cz', 'planetlab1.cs.uoregon.edu', 'planetlab3.wail.wisc.edu',
     'planetlab-js1.cert.org.cn', 'planetlab2.cs.unc.edu', 'planetlab03.cs.washington.edu', 'planetlab3.cesnet.cz',
     'planetlab-n1.wand.net.nz', 'planetlab1.dtc.umn.edu', 'node2.planetlab.mathcs.emory.edu', 'planetlab1.temple.edu',
     'saturn.planetlab.carleton.ca']
    print len(listx)

    os.chdir('/Users/yxyang/Documents/CPS512/final_data/')
    root = '/Users/yxyang/Documents/CPS512/final_data/'
    for root,dir,filenames in  os.walk(root):
        for filename in filenames[1:]:
            print time.localtime()
            x = codecs.open(filename,'r','utf-8-sig')
            lines = x.readlines()
            x.close()
            out_name = "/Users/yxyang/Documents/CPS512/final_data/"+filename[7:]
            fout = codecs.open(out_name, 'w', 'utf-8-sig')
            count = 0
            start = time.localtime()
            print "count = 0", start
            for line in lines:
                count += 1
                if count%15000 == 0:

                    delta_time = datetime.datetime.now() - start
                    start = datetime.datetime.now()
                    print "count = %s" % count, start
                    time.sleep(delta_time.total_seconds())

                try:
                    IPaddr = line.split(' ')[1][:-1]


                except:
                    print "IPaddr parse error",line

                d_name = line.split(' ')[0]


                #print IPaddr
                if(isIPaddr(IPaddr)):
                    content = parse_IP(IPaddr)
                    if type(content) == int:
                        print "error",IPaddr,content

                    #if content.startswith('<'):
                        #print content
                    else:
                        content = json.loads(content)
                        content['ip'] = IPaddr
                        content['d_name'] = d_name
                        #print content
                    #print content
                    content = json.dumps(content)
                    #print type(content)
                    ##write into txt files

                    str = content + '\n'

                    fout.write(str)

                    #TODO: ViSualize the content
                else:
                    print IPaddr

            fout.close()


if __name__ == '__main__':
    main()



