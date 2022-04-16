package com.yinpeng.hashmap;/*
其实这三个方法表示的是在访问、插入、删除某个节点之后，进行一些处理，
它们在LinkedHashMap都有各自的实现。
LinkedHashMap正是通过重写这三个方法来保证链表的插入、删除的有序性。
*/

    // Callbacks to allow LinkedHashMap post-actions
    void afterNodeAccess(Node<K,V> p) {
    /*
    ，就是把当前节点e移至链表的尾部。因为使用的是双向链表，所以在尾部插入可以以O（1）的时间复杂度来完成。并且只有当accessOrder设置为true时，才会执行这个操作。在HashMap的putVal方法中，就调用了这个方法
    */
    }
    void afterNodeInsertion(boolean evict) {
    /*
    afterNodeInsertion方法是在哈希表中插入了一个新节点时调用的，它会把链表的头节点删除掉，删除的方式是通过调用HashMap的removeNode方法。想一想，通过afterNodeInsertion方法和afterNodeAccess方法，是不是就可以简单的实现一个基于最近最少使用（LRU）的淘汰策略了？当然
    */
    }
    void afterNodeRemoval(Node<K,V> p) {
    /*
    这个方法是当HashMap删除一个键值对时调用的，它会把在HashMap中删除的那个键值对一并从链表中删除，保证了哈希表和链表的一致性。
    */
    }


//put调用的方法
    final V putVal(int hash, K key, V value, boolean onlyIfAbsent,
                   boolean evict) {
        Node<K,V>[] tab;//Hash表的局部变量，在第(tab = table)赋值
        Node<K,V> p;//当前hash下标下的Entry节点，在(p = tab[i = (n - 1) & hash])赋值
        int n, i;//n=扩容后的容量长度
        if (
            (tab = table) == null//如果table是空的
            ||
            (n = tab.length) == 0//如果tab的长度等于0
        )
            n = (tab = resize()).length;//n=扩容或者初始化后的长度
        if (
            (p = tab[i = (n - 1) & hash]) == null
            //n是集合容量，他的大小是2的n次幂，容量为什么是2的n次幂呢？因为
            //2^1=10
            //2^2=100
            //2^3=1000
            //2^4=10000
            //因为在计算桶下标的时候要可以更方便的计算2的n次方减一
            //为什么要计算2的n次方-1？因为10000-1=1111
            //这个时候可以可以直接跟hash值的后面4位进行按位与运算，更方便的计算桶下标：1000101010 1001&1111=1001
            //如果不是2的n次幂，后面的1111就会变成例如--》1010这样的数字，这个时候进行按位与运算，一定会有一部分桶下标没有值
            //1001&1010=1000  1110&1010=1010  所以0101的位置上面一定没有值
        )
            tab[i] = newNode(hash, key, value, null);//如果(p = tab[i = (n - 1) & hash]) == null 满足，说明这一个hash对应的桶下标没有值，所以直接创建一个节点
        else {//如果到这里说明：对应的hash值&cap-1索引位置的地方有值，要么是链表，要么是红黑树
            Node<K,V> e; K k;
            if (
                p.hash == hash //p是之前已经存在到tab[i = (n - 1) & hash]索引位置的值，hash是当前传进来的值，
                //不是说hash值一样才能存到一起吗？既然都走到这里了，为什么还要判断hash值为什么一样呢？
                //因为hash值并不直接等于存储的位置，也仅仅是2的n次方按位与hash计算出来的数据一样，也仅仅是二进制前几位数字一样而已，因为2^n-1=11111(二进制)
                &&
                (
                    (k = p.key) == key //p.key是当前已经存在在该hash&cap-1索引位置的值，将p的key赋值给K k，并且判断是否等于传进来的key
                    ||
                    (key != null //判断传进来的key不等于null
                        && key.equals(k)//判断传进来的k是不是在内存指向的值是否等于之前的k，为什么前面调用了==进行比较这里还是用equals进行比较呢？因为传进来的K对象可能自己重写了equals方法，所以用自己的equals方法再进行比较，也就是这个判断的意思是【hash值相等并且【key相等/equeals相等】】就可以判断key是完全相等的
                    )
                )
            )
            //这里仅仅是判断传进来的Node是不是与cap-1&hash索引下的node的头节点是不是一样，无法判断是否与链表体的某节点一样，后面有判断是否与链表体某节点一样的方法
                e = p;//经历了如上判断，这个时候就可以直接将p节点直接赋值给e，p是当前hash&cap-1索引对应的节点，e是新声明的节点
            else if (p instanceof TreeNode)//判断p是不是树节点
                e = ((TreeNode<K,V>)p).putTreeVal(this, tab, hash, key, value);//如果是树节点就走树话的putTreeValue
            else {//在否则就是正常的链表，就可以正常走链表的插入了
                for (int binCount = 0; ; ++binCount) {//循环遍历当前链表，如果满足if (binCount >= TREEIFY_THRESHOLD - 1)内的条件就树化
                    if ((e = p.next) == null) {//如果p.next等于null就满足
                        p.next = newNode(hash, key, value, null);//p是当前tab[hash&cap-1]的节点，在p的后面新建一个节点
                        if (binCount >= TREEIFY_THRESHOLD - 1) // TREEIFY_THRESHOLD - 1=7，因为binCount是从0开始的，0-7是8个数字，
                            treeifyBin(tab, hash);//去树话
                        break;//并且跳出循环
                    }
                    if (
                    e.hash == hash //e现在是p.next，也就是当前链表节点的下一个节点，能来到这里只能说明hash&cap-1是相等的，并不能说明hash值完全相等
                    &&
                        (
                            (k = e.key) == key //k等于当前节点的k，e是hash&cap-1的当前节点，判断e.key是不是与当前传进来的key相等
                            ||
                            (
                                key != null //判断key是否不等于null，问：为什么之前判断过这个，为什么现在还要判断？虽然之前也判断了但是并没有拦截，因为为null可能就继续向下走，所以每次都要在判断一次
                                &&
                                key.equals(k)//判断传进来的key是否等于cap-1&hash所对应的节点的k
                            )
                        )
                    )
                        break;//满足这个条件就会走到break【满足e.hash等于hash并且【e.key等于传进来的key或者【key不等于null并且key.equals(k)】】】
                    p = e;//p.next不是null并且【满足e.hash等于hash并且【e.key等于传进来的key或者【key不等于null并且key.equals(k)】】】走到这一步说明传进来的值与链表体（除了链表头，链表头是否与该key在之前已经判断过了）某一个链表的节点完全一样
                }
            }
            //走到这里面说明我第一次put的key与第二次put的key是一样的
            if (e != null) { // 走到这里e还是有可能是null的，因为走(e = p.next) == nul这个判断，e就是空的
                V oldValue = e.value;//把e.value赋值给oldValue
                if (!onlyIfAbsent || oldValue == null)//如果onlyIfAbsent为true或者oldValue不等于空，则不更改现有值
                    e.value = value;//e.value=value,e是当前节点e.value是当前节点的值，执行到这里说明放入的key与之前这个地方的key是完全一样的，因为是完全一样的，所以将e.value覆盖掉，覆盖成现在传进来的值
                afterNodeAccess(e);//在HashMap中没用，LinkedHashMap重写了这个方法将当前的e节点放到尾部
                return oldValue;//返回被覆盖的值
            }
        }
        /*
            由于HashMap不是线程安全的,所以在迭代的时候,会将modCount赋值到迭代器的expectedModCount属性中,然后进行迭代,
            如果在迭代的过程中HashMap被其他线程修改了,modCount的数值就会发生变化,
            这个时候expectedModCount和ModCount不相等,
            迭代器就会抛出ConcurrentModificationException()异常
        */
        ++modCount;
        if (++size > threshold)//size先加一，加一以后如果大于扩容阈值，就执行resize
            resize();
        afterNodeInsertion(evict);//在HashMap中没用，LinkedHashMap重写了这个方法
        return null;
    }




//扩容方法，初始化扩容的时候会调用
        final Node<K,V>[] resize() {
        Node<K,V>[] oldTab = table;//保存Hash表的局部变量
        int oldCap = (oldTab == null) ? 0 : oldTab.length;//int 老容量=(老容量==null)？0：老表.length;//如果上一张表是空，容量就设置成10，否则oldCap就设置成老表的长度
        int oldThr = threshold;//扩容的临界值，默认的计算方式是16*0.75=12（capacity()*DEFAULT_LOAD_FACTOR）
        int newCap, newThr = 0;//int 新的容量，旧的容量
        if (oldCap > 0) {//如果旧的容量大于0
        //这一个if块里面就做了两件事：临界值*2，容量*2
            if (oldCap >= MAXIMUM_CAPACITY) {//如果旧容量大于等于HashMap最大容量2的30次方
                threshold = Integer.MAX_VALUE;//扩容临界值=int最大值
                return oldTab;//直接返回旧的节点数
            }
            else if (
                (newCap = oldCap << 1)//(新的容量=旧的容量*2)
                < MAXIMUM_CAPACITY//如果旧的容量小于HashMap容量最大值，前半部分就返回true
                &&
                oldCap >= DEFAULT_INITIAL_CAPACITY//旧容量如果大于等于默认的初始容量就返回true
            ){//两个式子进行按位与运算
                newThr = oldThr << 1;//新的扩容临界值等于旧的临界值*2
            }
        }
        else if (oldThr > 0) // 如果老扩容阈值大于0
            newCap = oldThr;//新的容量=旧的扩容阈值//什么情况下走到这个位置？newHashMap时，map没有初始化，并且扩容阈值大于0，oldThr=16时是第一次进入
        else {               //来到这里说明扩容阈值也是0--零初始阈值表示使用默认值--第一次初始化的时候会来这里，上次的容量等于0，扩容阈值也是0
            newCap = DEFAULT_INITIAL_CAPACITY;//新的容量=16
            newThr = (int)(DEFAULT_LOAD_FACTOR * DEFAULT_INITIAL_CAPACITY);//新的扩容阈值=(int)0.75*16=12这就是第一次扩容阈值
        }
        if (newThr == 0) {//如果新的扩容阈值等于0
            float ft = (float)newCap * loadFactor;//ft=新的容量*0.75
            newThr = (//基本上直接返回新的扩容阈值=ft
                        newCap < MAXIMUM_CAPACITY //新的容量如果小于最大容量2的31次方
                        &&
                        ft < (float)MAXIMUM_CAPACITY ?(int)ft : Integer.MAX_VALUE);//ft<最大容量?ft:int最大值
        }
        threshold = newThr;//扩容阈值等于新的扩容阈值计算方式--->容量*负载因子
        @SuppressWarnings({"rawtypes","unchecked"})
        Node<K,V>[] newTab = (Node<K,V>[])new Node[newCap];//新表=new Node[新容量]
        table = newTab;//table成员变量表=新new的表
        if (oldTab != null) {//老table如果不等于null
            for (int j = 0; j < oldCap; ++j) {//循环遍历老的数组
                Node<K,V> e;
                if ((e = oldTab[j]) != null) {//将老数组的当前节点赋值给e，所以e就是当前遍历的节点，当前节点判断是不是空，不是空的话继续向下执行，如果是空的直接return newTab
                    oldTab[j] = null;//老数组节点=null//e现在是老数组
                    if (e.next == null)//e.next==null说明当前的e是一个尾巴节点，如果按照泊松分布，其实大多数的hash值都是尾巴节点
                        newTab[e.hash & (newCap - 1)] = e;//计算e在新hash表中的位置
                    else if (e instanceof TreeNode)//如果e是TreeNode的子类
                        ((TreeNode<K,V>)e).split(this, newTab, j, oldCap);//走TreeNode的树化退化的方法
                    else { // preserve order //保持顺序，这个是为了解决1.7中死锁的问题

                    //扩容前的老索引是 hash10101010&1111=1010
                    //计算新的扩容索引 因为2的n次方扩容了，所以比老容量的二进制多了一位1，所以新的计算方式是hash10101010&11111=01010，所以新的索引是01010
                    //为什么要这样呢？因为这样新加的一位是0或者1，也就是这个链表上面所计算的hash要么不改变要么改变到一个位置，所以一个链表只会分成两个链表，
                    //|--------------------------------|            |------------------------------------------------
                    //|       |        |       |       |----------> |     |     |     |     |     |     |     |     |
                    //|--------------------------------|            |------------------------------------------------
                    //    |                                            |                  |
                    //    □ 1                                          □ 2                □ 1
                    //    |                                            |                  |
                    //    □ 2                                          □ 4                □ 3
                    //    |
                    //    □ 3
                    //    |
                    //    □ 4

                        Node<K,V> loHead = null, loTail = null;//低位节点
                        Node<K,V> hiHead = null, hiTail = null;//高位节点
                        Node<K,V> next;//执行do while时，他代表当前的节点，进入dowhile会变成当前节点的下一个
                        do {

                        /////////////////////////////画图就清晰这个地方///////////////////////////////////

                            //假设：e.hash=10101001   oldCap=10000(16) 按位与运算=00000
                            //假设：e.hash=10111001   oldCap=10000     按位与运算=10000
                            next = e.next;//e = oldTab[j];e是旧的节点
                            //计算出来如果是低位就在这个代码块
                            if ((e.hash & oldCap) == 0) {//假设：e.hash=10101001   oldCap=10000(16) 按位与运算=00000//不动

                                if (loTail == null)//低位的尾巴节点是空，说明当前节点没有元素，是第一次循环，所以插入第一个元素
                                    loHead = e;//如果为节点等于空，那低位的头节点自然是当前节点e
                                else//低位尾巴节点不是空的
                                    loTail.next = e;//尾巴节点的下一个就是e当前节点，这个时候尾巴节点就不是了，而e就是尾巴节点了
                                loTail = e;//这个时候就把尾巴节点设置成e了，这个时候上一行的loTail就不是尾巴节点了

                            }
                            //计算出来如果是低位就在这个代码块
                            else {//假设：e.hash=10111001   oldCap=10000     按位与运算=10000//移向高位
                                if (hiTail == null)//如果高位的尾巴节点是空的，说明这是第一个元素
                                    hiHead = e;//因为是第一个元素，所以高位节点的头节点是当前节点
                                else
                                    hiTail.next = e;//否则高位节点的尾巴节点的下一个是e当前节点
                                hiTail = e;//高位节点的尾巴节点为当前节点
                            }
                        } while ((e = next) != null);//当e=next并且不等于空的时候一直循环，这是在循环链表，能进入到这里面就说明已经是链表了，e是当前所遍历的节点
                        if (loTail != null) {//如果低位的尾巴节点不是null，说明刚刚遍历完一个链表，并且loTail就是新hash链表的尾巴节点
                            loTail.next = null;//所以将此尾巴节点的下一个节点设置成null
                            newTab[j] = loHead;//然后新表的新索引为低位的头节点   j的大小一定与扩容前的该属性的索引的位置一样
                        }
                        if (hiTail != null) {//如果高位的尾巴节点不是null，说明刚刚遍历完一个链表，并且hiTail就是新hash链表的尾巴节点
                            hiTail.next = null;//所以将此尾巴节点的下一个节点设置成null
                            newTab[j + oldCap] = hiHead;//然后高位节点的新的桶下标是老hash表中的数组下标加容量=新的桶下标的位置
                        }
                    }
                }
            }
        }
        return newTab;//将当前newTab返回，扩容完毕初始化完毕
    }
