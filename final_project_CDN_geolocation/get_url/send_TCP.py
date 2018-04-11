from scapy.all import *

dst = '203.91.121.82'


#option 0 EOL
#hop3 blocked, hop 1,2,4,5,6,7,8,9,10 all right

#option 1 NOP
#hop3 blocked

#option3
sport = random.randint(1024,65535)
ip=IP(dst=dst, ttl=64)
#tcp_SYN = ip/TCP(dport=80,sport=sport, flags='S',seq=1000)
#tcp_SYN = ip/TCP(dport=80,sport=sport, flags='S',seq=1000,  options=[('Timestamp',(0, 0))])
#tcp_SYN = ip/TCP(dport=80,sport=sport, flags='S',seq=1000,  options=[('SackOK',"")])
#tcp_SYN = ip/TCP(dport=80,sport=sport, flags='S',seq=1000,  options=[('Experiment',0xf989,0xcafe,0x0102,0x0002)])


#result = sr1(tcp_SYN, timeout=3)
tcp_SYN = ip/TCP(dport=80,sport=sport,seq=1000,  options=[("MSS",100)])

#tcp_SYN = ip/TCP(dport=80,sport=sport,seq=1000,  options=[("SAckOK", 2)])
#tcp_SYN = ip/TCP(dport=80,sport=sport,seq=1000,  options=[('SAck',(0x6,0x23,0x12,0x1A,0x22))])/"data"
#tcp_SYN = ip/TCP(dport=80,sport=sport,seq=1000,  options=[(25,"\x04\x3A\x29")])1
#tcp_SYN = ip/TCP(dport=80,sport=sport,seq=1000,  options=[("NOP", ""),(19,"\xff\xff\xff\xff\xff\xff")])

#tcp_SYN = ip/TCP(dport=80,sport=sport,seq=1000,  options=[('AltChkSum',(0x03,0x00))])

tcp_SYN.show2()
result = sr1(tcp_SYN, timeout=3)
#print [res.show for res in result]
print result.show()