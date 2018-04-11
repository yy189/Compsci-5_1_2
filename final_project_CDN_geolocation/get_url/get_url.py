# -*- coding: utf-8 -*-

import pexpect
import threading
import codecs
import json
import sys
import time


node_list = [
    'planetlab02.cs.washington.edu', 'planetlab-5.eecs.cwru.edu', 'planetlab4.mini.pw.edu.pl', 'planetlab4.postel.org',
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
     'saturn.planetlab.carleton.ca'
]




#login
'''
def login(server):
    command = "ssh -i ~/.ssh/id_rsa duke_quantify_openness@%s" % server
    ssh = pexpect.spawn(command)
    try:
        i = ssh.expect(['not known', 'continue connecting (yes/no)?', '~', 'timed out', 'unreachable', 'keyboard-interactive'])
        print server, i
        if i == 1:
            ssh.sendline('yes')
            j = ssh.expect(['duke_quantify_openness', 'keyboard-interactive'])
            time.sleep(1)
            if j == 0:
                ssh.sendline("logout")
                valid_servers[count] = 1
        elif i == 2:
            ssh.sendline("logout")
            valid_servers[count] = 1
        ssh.close()
    except pexpect.EOF:
        ssh.close()
    except pexpect.TIMEOUT:
        ssh.close()
'''

def scp_package(server):
    command = "scp -i /Users/yxyang/.ssh/id_rsa -r /Users/yxyang/Downloads/youtube_crawler duke_quantify_openness@%s:~" % server

    try:
        output = pexpect.run(command)
        #i = scp.expect(['not known', 'continue connecting (yes/no)?', '~', 'timed out', 'unreachable', 'keyboard-interactive'])

        print server
        print output
        #output.sendline("yes")
        #print exit1
        time.sleep(3)

    except Exception as e:
        print e






def main():
    #for server in node_list:
        scp_package(node_list[0])
        #login(server)


if __name__ == '__main__':
    main()
