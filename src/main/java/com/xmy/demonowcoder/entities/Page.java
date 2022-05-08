package com.xmy.demonowcoder.entities;

/**
 * @author xumingyu
 * @date 2022/4/14
 * 封装分页相关的信息
 **/
public class Page {

    //当前页面
    private int current=1;
    //显示上限
    private int limit = 6;
    //数据总数(用于计算总页数)
    private int rows;
    //查询路径(用于复用分页链接)
    private String path;

    public int getCurrent() {
        return current;
    }

    /**
     * 设置当前
     *
     * @param current
     */
    public void setCurrent(int current) {
        //要作输入判断
        if (current >= 1) {
            this.current = current;
        }
    }

    public int getLimit() {
        return limit;
    }

    /**
     * 设置一页最多几条
     *
     * @param limit
     */
    public void setLimit(int limit) {
        if (limit >= 1 && limit <= 100) {
            this.limit = limit;
        }
    }

    public int getRows() {
        return rows;
    }

    /**
     * 设置总条数
     *
     * @param rows
     */
    public void setRows(int rows) {
        if (rows >= 0) {
            this.rows = rows;
        }
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }


    /**
     * 获取当前页的起始行
     **/
    public int getOffset(){
        //current*limit-limit
        return (current-1)*limit;
    }

    /**
     * 获取总页数
     **/
    public int getTotal(){
        //rows/limit[+1]
        if (rows%limit==0){
            return rows/limit;
        }else{
            return rows/limit+1;
        }
    }

    /**
     * 获取起始页码
     **/
    public int getFrom(){
        int from=current-2;
        return from < 1 ? 1 : from;
    }

    /**
     * 获取结束页码
     **/
    public int getTo(){
        int to=current+2;
        int total=getTotal();
        return to > total ? total : to;
    }
}

