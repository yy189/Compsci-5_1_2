

import time
import os


if __name__ == '__main__':

    files = []
    files2 = []
    root = '/Users/yxyang/Documents/CPS512/final_project/'
    os.chdir('/Users/yxyang/Documents/CPS512/final_project/')
    for root, dir, filenames in os.walk(root):
        for filename in filenames[1:]:
            count = len(open(filename, 'rU').readlines())
            #print filename,count
            files.append(filename[7:])

    root = '/Users/yxyang/Documents/CPS512/final_project2/'
    os.chdir('/Users/yxyang/Documents/CPS512/final_project2/')
    for root, dir, filenames in os.walk(root):
        for filename in filenames[1:]:
            count = len(open(filename, 'rU').readlines())
            #print filename,count
            files2.append(filename[7:])

            ele = filename[7:]
            if ele not in files:
                print ele,"not in 47"
            if ele in files:
                print ele









