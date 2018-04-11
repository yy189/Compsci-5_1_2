# -*- coding: utf-8 -*－

import pexpect
import threading
import codecs
import json
import sys
import time
import threading

def scp_package(server):
    command = "scp -i /Users/yxyang/.ssh/id_rsa -r /Users/yxyang/Downloads/youtube_crawler6/get_delay.py duke_quantify_openness@%s:~" % server

    try:
        output = pexpect.run(command)

        time.sleep(5)

    except Exception as e:
        print e


def install(count, server):
    #scp_package(server)


    #command = "scp -i /Users/yxyang/.ssh/id_rsa -r /Users/yxyang/Downloads/youtube_crawler6/get_delay.py duke_quantify_openness@%s:~/youtube_crawler6/" % server
    #command = "scp -i /Users/yxyang/.ssh/id_rsa -r duke_quantify_openness@%s:~/youtube_crawler6/IPaddrs%s.txt /Users/yxyang/Documents/CPS512/final_project/" % (server,server)
    command = "scp -i /Users/yxyang/.ssh/id_rsa -r duke_quantify_openness@%s:~/youtube_crawler6/Delays_%s.txt /Users/yxyang/Documents/CPS512/final_project2/" % (server,server)

    scp = pexpect.spawn(command)
    i = -1
    try:
        i = scp.expect(['not known','continue connecting (yes/no)?'])
        print "i= ",i
    except:
        #print "i=error???",scp
        print '.'
    try:
        if i==1:
            print server
            scp.sendline("yes")
        #scp.sendline("yes")
        time.sleep(1)
        print server, " done"


    except Exception as e:
        print "scp Error",server,e
    scp.close()

    '''
    command = "ssh -i ~/.ssh/id_rsa duke_quantify_openness@%s" % server
    ssh = pexpect.spawn(command)
    try:
        #ssh.sendline("yes")
        #time.sleep(1)
        ssh.expect('duke_quantify_openness')
        time.sleep(1)
        print "ssh to %s success" % server
        ssh.sendline("cd youtube_crawler6")
        time.sleep(1)
        ssh.sendline("cp Delays.txt Delays_%s.txt" % server)
        time.sleep(1)
        #ssh.sendline("cp IPaddrs.txt IPaddrs%s.txt" % server)
        #ssh.sendline("nohup sudo bash install6.sh &")
        #ssh.sendline("nohup sudo python2.7 youtube_crawler.py &")
        #ssh.sendline("nohup sudo python2.7 get_delay.py &")
        #ssh.expect("nohup.out")
        #time.sleep(1)
        #ssh.sendline("\n")
        #time.sleep(1)
        ssh.sendline("logout")
    except pexpect.EOF:
        print server, 'EOF'
        ssh.close()
    except pexpect.TIMEOUT:
        print server, 'TIMEOUT'
        ssh.close()
    '''




def install_scapy():
    #listx = ['planetlab02.cs.washington.edu']
    #install('planetlab02.cs.washington.edu')

    listx = ['planetlab02.cs.washington.edu','planetlab-5.eecs.cwru.edu', 'planetlab2.c3sl.ufpr.br', 'planetlab4.mini.pw.edu.pl', 'planetlab4.postel.org', 'node1.planetlab.albany.edu', 'planetlab1.pop-pa.rnp.br', 'planetlab5.eecs.umich.edu', 'plink.cs.uwaterloo.ca', 'planetlab5.williams.edu', 'planetlab1.cs.purdue.edu', 'planetlab1.postel.org', 'planetlab-n2.wand.net.nz', 'planetlab2.pop-pa.rnp.br', 'planetlab-2.calpoly-netlab.net', 'planetlab1.cesnet.cz', 'whitefall.planetlab.cs.umd.edu', 'planetlab3.williams.edu', 'planetlab2.dtc.umn.edu', 'pl1.eng.monash.edu.au', 'planetlab1.cs.ubc.ca', 'pl2.ucs.indiana.edu', 'planetlab1.utdallas.edu', 'lefthand.eecs.harvard.edu', 'planetlabone.ccs.neu.edu', 'pl1.ucs.indiana.edu', 'pl2.eng.monash.edu.au', 'planetlab3.rutgers.edu', 'planetlab04.cs.washington.edu', 'planetlab3.cs.uoregon.edu', 'cs-planetlab3.cs.surrey.sfu.ca', 'planetlab4.cs.uoregon.edu', 'planetlab1.pop-mg.rnp.br', 'planetlab-1.scie.uestc.edu.cn', 'pl1.rcc.uottawa.ca', 'planetlab3.eecs.umich.edu', 'planetlab4.williams.edu', 'planetlab2.pop-mg.rnp.br', 'node1.planetlab.mathcs.emory.edu', 'planetlab5.ie.cuhk.edu.hk', 'planetlab1.koganei.itrc.net', 'planetlab2.koganei.itrc.net', 'planetlab3.comp.nus.edu.sg', 'planetlab1.unr.edu', 'planetlab01.cs.washington.edu', 'planet-lab4.uba.ar', 'planetlab2.inf.ethz.ch', 'planetlab2.utdallas.edu', 'planetlab2.cs.purdue.edu', 'planetlab1.comp.nus.edu.sg', 'planetlab2.cesnet.cz', 'planetlab1.cs.uoregon.edu', 'planetlab3.wail.wisc.edu', 'planetlab-js1.cert.org.cn', 'planetlab-03.cs.princeton.edu', 'planetlab2.cs.unc.edu', 'planetlab03.cs.washington.edu', 'planetlab3.cesnet.cz', 'planetlab-n1.wand.net.nz', 'planetlab1.dtc.umn.edu', 'node2.planetlab.mathcs.emory.edu', 'planetlab-04.cs.princeton.edu', 'planetlab1.temple.edu']
    listxq = ['planetlab2.c3sl.ufpr.br',
'node1.planetlab.mathcs.emory.edu',
'planetlab1.koganei.itrc.net',
'pl1.rcc.uottawa.ca',
'planetlab4.williams.edu',
'planetlab01.cs.washington.edu',
'planetlab3.comp.nus.edu.sg',
'planetlab2.inf.ethz.ch',
'planetlab2.cs.purdue.edu',
'planetlab2.cesnet.cz',
'planetlab3.wail.wisc.edu',
'planetlab-03.cs.princeton.edu',
'planetlab-n1.wand.net.nz',
'planetlab4.cs.uoregon.edu',
'planetlab3.cesnet.cz',
'planetlab-1.scie.uestc.edu.cn',
'planetlab3.cs.uoregon.edu',
'planetlab5.ie.cuhk.edu.hk',
'planetlab3.eecs.umich.edu',
'planetlab2.koganei.itrc.net',
'planetlab2.pop-mg.rnp.br',
'planetlab1.unr.edu',
'planet-lab4.uba.ar',
'planetlab2.utdallas.edu',
'planetlab1.cs.uoregon.edu',
'planetlab2.cs.unc.edu',
'planetlab-js1.cert.org.cn',
'planetlab1.comp.nus.edu.sg',
'planetlab1.dtc.umn.edu',
'lefthand.eecs.harvard.edu',
'node2.planetlab.mathcs.emory.edu',
'planetlab-04.cs.princeton.edu',
'planetlab1.temple.edu']



    listy = []
    for i in range(len(listx)):
        listy.append([i, listx[i]])
    n = len(listx)/30
    sp_listx = []
    x = 0
    for i in range(30):
        if i != 29:
            sp_listx.append(listy[x:x+n])
            x += n
        else:
            sp_listx.append(listy[x:len(listy)])

    thread_list = []
    for i in range(30):
        t = threading.Thread(target=thread_control, args=(sp_listx[i], install, ))
        thread_list.append(t)
    for t in thread_list:
        t.start()
    for t in thread_list:
        t.join()


def thread_control(listx, function):
    for i in listx:
        function(i[0], i[1])


#def run_get_url():



if __name__ == '__main__':
    install_scapy()
    #run_get_url()

