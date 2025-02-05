from pylab import *
'''
Now here is a different option, that is exact and maybe as fast, but requires more memory.
we can figure out from the recurrence relationship, by unrolling the decay function

f(t) = f(t-1)*(decay)
f(t-1) = f(t-2)*(decay)
...
f(0) = k
so f(3) = (((k*decay)*decay)*decay)
which has the simple pattern
f(b) = (((...(k)*decay)*decay ...) * decay)
=>
f(t) = k*decay^t

so we have lots of buckets, that are mostly empty, and a few that have a good number 
of items in them. We can calculate the exact decay using the above equation for at 
any time t. If the array of counters was an array of pointers to a list of numbers 
that represent the time at which a collision occured, we could use the above equation 
to compute the exact decayed value as the sum of decaying functions.
Since really old stuff gets exponentially smaller, we can set a threshold 
on the lists length, pruning things that are really old.
eg .9^100 = 2.66e-5
'''


def updateDecayCalc(prev,decayrate):
    '''
        this is the standard memoryless decay and update function
        incrementally decay all data evenly. this function serves as our objective
        alternatively : return (prev - (prev*decayrate*(1.0/float(mod))))
    '''
    return prev * (1.0-decayrate)


def decayOnInsert(prev_val,prevt,t,decayrate):
    ss = prev_val*(1-decayrate)**(t-prevt)
    return ss


def checkOrder(fullSeq,sparseSeq,indeces):
    '''
        check the order of the  sequences relative to the correct ordering A, and our approximate ordering B
        return the correct order/all order ratio
    '''
    sums = 0
    for i in range(len(indeces[0])):
        fseq = []
        for fit in range(len(indeces)):
            fseq.append((fullSeq[fit][indeces[fit][i]] ,fit))
        sseq = []
        for fit in range(len(indeces)):
            sseq.append((sparseSeq[fit][i] ,fit))

        s = [l for (k,l) in sorted(fseq)]
        t = [l for (k,l) in sorted(sseq)]
        sums+= int(s==t)
    return sums/float(len(indeces[0]))

def plotData(exactSequence,insertPts,approxInsertPts,order,n):
    '''
        plot the exact decay graph along with the approximate decay data.
        approximate data only occurs when a vector is added.
    '''
    colors = ["blue","green","red","orange","gray","purple","brown","yellow","pink","black","tan"]
    for seqit in range(len(exactSequence)):
        plt.plot(range(n),exactSequence[seqit],color=colors[seqit])
    for seqit in range(len(insertPts)):
        plt.plot(insertPts[seqit],approxInsertPts[seqit],'x',color=colors[seqit])
    plt.show()

def run(mod,plotdata=False):
    '''
        mod: the stream cluster interval
        n: number of data points to simulate
        arrivalrate: the speed at which we should generate hits to a bucket
        nseq: number of sequences to consider
        decayrate: the decay rate of a bucket
        Compute the decay rates and randomly add hash hits to the representative buckets
    '''
    n = 100000
    nseq = 4
    decayrate = .999/float(mod)
    print decayrate
    arrivalrate = .001
    order = []#the order between the decays lists
    from random import random, randrange
    exactSequences = []
    
    approxInsertPts = []
    insertPts = {}

    
    for i in range(nseq):#generate our sequence arrays
        exactSequences.append([0.0]*n)
        approxInsertPts.append([0.0])
        insertPts[i] = [0]

    for i in range(1,n):#iterate over the data
        for j in range(nseq):#iterate over the number of sequences to consider
            exactSequences[j][i] = updateDecayCalc(exactSequences[j][i-1],decayrate)

        if random() < arrivalrate:#randomly add some hits to the buckets
            r = randrange(0,nseq)
            exactSequences[r][i]+=1

            #this is the only time we are allowed to run our aging calculation
            #upon insert lets update our order

            '''
            #update all mode 
            for jj in range(nseq):
                approxInsertPts[jj].append(decayOnInsert(approxInsertPts[jj][-1],insertPts[jj][-1],i,decayrate))
                insertPts[jj].append(i)
            '''

            #'''
            #update only new        
            approxInsertPts[r].append(decayOnInsert(approxInsertPts[r][-1],insertPts[r][-1],i,decayrate))
            insertPts[r].append(i)
            #'''
            approxInsertPts[r][-1]+=1

    #store correctness of order after each data point
    if plotdata: plotData(exactSequences,insertPts,approxInsertPts,order,n)

    return  checkOrder(exactSequences,approxInsertPts,insertPts)


def runmany():
    '''
         update the batch decay range for i in 1:1000
         plot the results with the batch decay as the x axis, and
         accuracy as the y axis
    '''
    #uncomment below to generate a per interval profile of your function
    print "interval accuracy"
    p = []
    for i in range(1,1000,1):
        p.append(run(i))
        print i,p[-1]
    plt.plot(range(1,1000,1),p,'o')
    plt.show()

t=10000
print "we are correct about order: " + str(run(t,True)*100) +"% of the time with interval at: " +str(t)
runmany()


