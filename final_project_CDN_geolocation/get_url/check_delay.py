#!python
# -*- coding:utf-8 -*-
import time

import urllib2
import httplib2
import sys
import urlparse
import socket
import codecs
from pytube import YouTube
import datetime


reload(sys)
sys.setdefaultencoding('utf-8')
# Hint: to fix "UnicodeDecodeError: 'ascii' code can't decode byte 0xc4 in position 0: ordinal not in range(128)"

h = httplib2.Http(disable_ssl_certificate_validation=True, timeout=0.1)



AUTO_RECONNECT_TIMES = 5

def crawl_OSN_profile(url):
    success = 0
    retry = 0

    while (success == 0):
        try:
            resp, content = h.request(url, "GET")
            #print username, resp['status']
            success = 1
            if (resp['status'] != '200'):
                return -1

        except:
            time.sleep(3)
            retry += 1
            #print '[rerty]'
            if (retry == AUTO_RECONNECT_TIMES):
                return -2
    return content

def getblock(s, s1, s2, x, i):
    y = x
    while True:
        x1 = s.find(s1, y)
        x2 = s.find(s2, y)
        if x1 == -1:
            x1 = 10000000
        if x2 == -1:
            x2 = 10000000
        x1 += 1
        x2 += 1
        if x1 < x2:
            i += 1
            y = x1
        else:
            i -= 1
            y = x2
        if i == 0:
            break
    return s[x:y+len(s2)-1]

def visit(url):
    start = datetime.datetime.now()
    request = urllib2.Request(url)
    data = urllib2.urlopen(request)
    delay = datetime.datetime.now() - start
    #addr = socket.gethostbyname(urlparse.urlparse(data.geturl()).hostname)
    #print "addr",addr
    return delay


def get_delay(url):
    count = 0
    try:

        delay = visit(url)
        # print "dstIP1",dstIP
        return delay
    except Exception as e:
        print e
        count += 1
        delay = -1
        print count
        return delay



#crawl the youtube website for video urls
def main():

    count = 0
    f = codecs.open("urls.txt","r",'utf-8-sig')
    x = f. readlines()
    f.close()

    fout = codecs.open("Delays.txt", "w", 'utf-8-sig')
    for url in x:
        url = unicode.strip(url)
        count += 1
        if count % 100 == 0:
            print count
        try:
            ip1 = get_delay(YouTube(url).streams.first().url)
            # url, ip1
            if ip1 is not None:
                if count % 50 == 0:
                    print "success", count
                line = url + " " + str(ip1) + '\n'
                fout.write(line)
        except Exception as e:
            print url, e
            continue
    fout.close()


if __name__ == '__main__':
    main()