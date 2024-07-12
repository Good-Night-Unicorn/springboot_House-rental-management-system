
package com.controller;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.text.SimpleDateFormat;
import com.alibaba.fastjson.JSONObject;
import java.util.*;
import org.springframework.beans.BeanUtils;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.context.ContextLoader;
import javax.servlet.ServletContext;
import com.service.TokenService;
import com.utils.*;
import java.lang.reflect.InvocationTargetException;

import com.service.DictionaryService;
import org.apache.commons.lang3.StringUtils;
import com.annotation.IgnoreAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.baomidou.mybatisplus.mapper.Wrapper;
import com.entity.*;
import com.entity.view.*;
import com.service.*;
import com.utils.PageUtils;
import com.utils.R;
import com.alibaba.fastjson.*;

/**
 * 租房房源
 * 后端接口
 * @author
 * @email
*/
@RestController
@Controller
@RequestMapping("/fangyuan")
public class FangyuanController {
    private static final Logger logger = LoggerFactory.getLogger(FangyuanController.class);

    private static final String TABLE_NAME = "fangyuan";

    @Autowired
    private FangyuanService fangyuanService;


    @Autowired
    private TokenService tokenService;

    @Autowired
    private BaoxiuService baoxiuService;//报修
    @Autowired
    private DictionaryService dictionaryService;//字典
    @Autowired
    private FangyuanCommentbackService fangyuanCommentbackService;//租房评价
    @Autowired
    private FangyuanOrderService fangyuanOrderService;//房源租赁
    @Autowired
    private FangyuanYuyueService fangyuanYuyueService;//租房预约
    @Autowired
    private ForumService forumService;//论坛
    @Autowired
    private GonggaoService gonggaoService;//公告
    @Autowired
    private LiuyanService liuyanService;//投诉建议
    @Autowired
    private YonghuService yonghuService;//用户
    @Autowired
    private ZufanghetongService zufanghetongService;//租房合同
    @Autowired
    private UsersService usersService;//管理员


    /**
    * 后端列表
    */
    @RequestMapping("/page")
    public R page(@RequestParam Map<String, Object> params, HttpServletRequest request){
        logger.debug("page方法:,,Controller:{},,params:{}",this.getClass().getName(),JSONObject.toJSONString(params));
        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(false)
            return R.error(511,"永不会进入");
        else if("用户".equals(role))
            params.put("yonghuId",request.getSession().getAttribute("userId"));
        params.put("fangyuanDeleteStart",1);params.put("fangyuanDeleteEnd",1);
        CommonUtil.checkMap(params);
        PageUtils page = fangyuanService.queryPage(params);

        //字典表数据转换
        List<FangyuanView> list =(List<FangyuanView>)page.getList();
        for(FangyuanView c:list){
            //修改对应字典表字段
            dictionaryService.dictionaryConvert(c, request);
        }
        return R.ok().put("data", page);
    }

    /**
    * 后端详情
    */
    @RequestMapping("/info/{id}")
    public R info(@PathVariable("id") Long id, HttpServletRequest request){
        logger.debug("info方法:,,Controller:{},,id:{}",this.getClass().getName(),id);
        FangyuanEntity fangyuan = fangyuanService.selectById(id);
        if(fangyuan !=null){
            //entity转view
            FangyuanView view = new FangyuanView();
            BeanUtils.copyProperties( fangyuan , view );//把实体数据重构到view中
            //修改对应字典表字段
            dictionaryService.dictionaryConvert(view, request);
            return R.ok().put("data", view);
        }else {
            return R.error(511,"查不到数据");
        }

    }

    /**
    * 后端保存
    */
    @RequestMapping("/save")
    public R save(@RequestBody FangyuanEntity fangyuan, HttpServletRequest request){
        logger.debug("save方法:,,Controller:{},,fangyuan:{}",this.getClass().getName(),fangyuan.toString());

        String role = String.valueOf(request.getSession().getAttribute("role"));
        if(false)
            return R.error(511,"永远不会进入");

        Wrapper<FangyuanEntity> queryWrapper = new EntityWrapper<FangyuanEntity>()
            .eq("fangyuan_name", fangyuan.getFangyuanName())
            .eq("fangyuan_types", fangyuan.getFangyuanTypes())
            .eq("fangyuan_kucun_number", fangyuan.getFangyuanKucunNumber())
            .eq("fangyuan_delete", fangyuan.getFangyuanDelete())
            ;

        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        FangyuanEntity fangyuanEntity = fangyuanService.selectOne(queryWrapper);
        if(fangyuanEntity==null){
            fangyuan.setFangyuanDelete(1);
            fangyuan.setCreateTime(new Date());
            fangyuanService.insert(fangyuan);
            return R.ok();
        }else {
            return R.error(511,"表中有相同数据");
        }
    }

    /**
    * 后端修改
    */
    @RequestMapping("/update")
    public R update(@RequestBody FangyuanEntity fangyuan, HttpServletRequest request) throws NoSuchFieldException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        logger.debug("update方法:,,Controller:{},,fangyuan:{}",this.getClass().getName(),fangyuan.toString());
        FangyuanEntity oldFangyuanEntity = fangyuanService.selectById(fangyuan.getId());//查询原先数据

        String role = String.valueOf(request.getSession().getAttribute("role"));
//        if(false)
//            return R.error(511,"永远不会进入");
        if("".equals(fangyuan.getFangyuanPhoto()) || "null".equals(fangyuan.getFangyuanPhoto())){
                fangyuan.setFangyuanPhoto(null);
        }

            fangyuanService.updateById(fangyuan);//根据id更新
            return R.ok();
    }



    /**
    * 删除
    */
    @RequestMapping("/delete")
    public R delete(@RequestBody Integer[] ids, HttpServletRequest request){
        logger.debug("delete:,,Controller:{},,ids:{}",this.getClass().getName(),ids.toString());
        List<FangyuanEntity> oldFangyuanList =fangyuanService.selectBatchIds(Arrays.asList(ids));//要删除的数据
        ArrayList<FangyuanEntity> list = new ArrayList<>();
        for(Integer id:ids){
            FangyuanEntity fangyuanEntity = new FangyuanEntity();
            fangyuanEntity.setId(id);
            fangyuanEntity.setFangyuanDelete(2);
            list.add(fangyuanEntity);
        }
        if(list != null && list.size() >0){
            fangyuanService.updateBatchById(list);
        }

        return R.ok();
    }


    /**
     * 批量上传
     */
    @RequestMapping("/batchInsert")
    public R save( String fileName, HttpServletRequest request){
        logger.debug("batchInsert方法:,,Controller:{},,fileName:{}",this.getClass().getName(),fileName);
        Integer yonghuId = Integer.valueOf(String.valueOf(request.getSession().getAttribute("userId")));
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            List<FangyuanEntity> fangyuanList = new ArrayList<>();//上传的东西
            Map<String, List<String>> seachFields= new HashMap<>();//要查询的字段
            Date date = new Date();
            int lastIndexOf = fileName.lastIndexOf(".");
            if(lastIndexOf == -1){
                return R.error(511,"该文件没有后缀");
            }else{
                String suffix = fileName.substring(lastIndexOf);
                if(!".xls".equals(suffix)){
                    return R.error(511,"只支持后缀为xls的excel文件");
                }else{
                    URL resource = this.getClass().getClassLoader().getResource("static/upload/" + fileName);//获取文件路径
                    File file = new File(resource.getFile());
                    if(!file.exists()){
                        return R.error(511,"找不到上传文件，请联系管理员");
                    }else{
                        List<List<String>> dataList = PoiUtil.poiImport(file.getPath());//读取xls文件
                        dataList.remove(0);//删除第一行，因为第一行是提示
                        for(List<String> data:dataList){
                            //循环
                            FangyuanEntity fangyuanEntity = new FangyuanEntity();
//                            fangyuanEntity.setFangyuanName(data.get(0));                    //房源名称 要改的
//                            fangyuanEntity.setFangyuanUuidNumber(data.get(0));                    //房源编号 要改的
//                            fangyuanEntity.setFangyuanPhoto("");//详情和图片
//                            fangyuanEntity.setFangyuanTypes(Integer.valueOf(data.get(0)));   //房源类型 要改的
//                            fangyuanEntity.setFangyuanKucunNumber(Integer.valueOf(data.get(0)));   //可以时长/月 要改的
//                            fangyuanEntity.setFangyuanNewMoney(data.get(0));                    //租赁价格/月 要改的
//                            fangyuanEntity.setFangyuanContent("");//详情和图片
//                            fangyuanEntity.setFangyuanDelete(1);//逻辑删除字段
//                            fangyuanEntity.setCreateTime(date);//时间
                            fangyuanList.add(fangyuanEntity);


                            //把要查询是否重复的字段放入map中
                                //房源编号
                                if(seachFields.containsKey("fangyuanUuidNumber")){
                                    List<String> fangyuanUuidNumber = seachFields.get("fangyuanUuidNumber");
                                    fangyuanUuidNumber.add(data.get(0));//要改的
                                }else{
                                    List<String> fangyuanUuidNumber = new ArrayList<>();
                                    fangyuanUuidNumber.add(data.get(0));//要改的
                                    seachFields.put("fangyuanUuidNumber",fangyuanUuidNumber);
                                }
                        }

                        //查询是否重复
                         //房源编号
                        List<FangyuanEntity> fangyuanEntities_fangyuanUuidNumber = fangyuanService.selectList(new EntityWrapper<FangyuanEntity>().in("fangyuan_uuid_number", seachFields.get("fangyuanUuidNumber")).eq("fangyuan_delete", 1));
                        if(fangyuanEntities_fangyuanUuidNumber.size() >0 ){
                            ArrayList<String> repeatFields = new ArrayList<>();
                            for(FangyuanEntity s:fangyuanEntities_fangyuanUuidNumber){
                                repeatFields.add(s.getFangyuanUuidNumber());
                            }
                            return R.error(511,"数据库的该表中的 [房源编号] 字段已经存在 存在数据为:"+repeatFields.toString());
                        }
                        fangyuanService.insertBatch(fangyuanList);
                        return R.ok();
                    }
                }
            }
        }catch (Exception e){
            e.printStackTrace();
            return R.error(511,"批量插入数据异常，请联系管理员");
        }
    }



    /**
    * 个性推荐
    */
    @IgnoreAuth
    @RequestMapping("/gexingtuijian")
    public R gexingtuijian(@RequestParam Map<String, Object> params, HttpServletRequest request){
        logger.debug("gexingtuijian方法:,,Controller:{},,params:{}",this.getClass().getName(),JSONObject.toJSONString(params));
        CommonUtil.checkMap(params);
        List<FangyuanView> returnFangyuanViewList = new ArrayList<>();

        //查询订单
        Map<String, Object> params1 = new HashMap<>(params);params1.put("sort","id");params1.put("yonghuId",request.getSession().getAttribute("userId"));
        PageUtils pageUtils = fangyuanOrderService.queryPage(params1);
        List<FangyuanOrderView> orderViewsList =(List<FangyuanOrderView>)pageUtils.getList();
        Map<Integer,Integer> typeMap=new HashMap<>();//购买的类型list
        for(FangyuanOrderView orderView:orderViewsList){
            Integer fangyuanTypes = orderView.getFangyuanTypes();
            if(typeMap.containsKey(fangyuanTypes)){
                typeMap.put(fangyuanTypes,typeMap.get(fangyuanTypes)+1);
            }else{
                typeMap.put(fangyuanTypes,1);
            }
        }
        List<Integer> typeList = new ArrayList<>();//排序后的有序的类型 按最多到最少
        typeMap.entrySet().stream().sorted((o1, o2) -> o2.getValue() - o1.getValue()).forEach(e -> typeList.add(e.getKey()));//排序
        Integer limit = Integer.valueOf(String.valueOf(params.get("limit")));
        for(Integer type:typeList){
            Map<String, Object> params2 = new HashMap<>(params);params2.put("fangyuanTypes",type);
            PageUtils pageUtils1 = fangyuanService.queryPage(params2);
            List<FangyuanView> fangyuanViewList =(List<FangyuanView>)pageUtils1.getList();
            returnFangyuanViewList.addAll(fangyuanViewList);
            if(returnFangyuanViewList.size()>= limit) break;//返回的推荐数量大于要的数量 跳出循环
        }
        //正常查询出来商品,用于补全推荐缺少的数据
        PageUtils page = fangyuanService.queryPage(params);
        if(returnFangyuanViewList.size()<limit){//返回数量还是小于要求数量
            int toAddNum = limit - returnFangyuanViewList.size();//要添加的数量
            List<FangyuanView> fangyuanViewList =(List<FangyuanView>)page.getList();
            for(FangyuanView fangyuanView:fangyuanViewList){
                Boolean addFlag = true;
                for(FangyuanView returnFangyuanView:returnFangyuanViewList){
                    if(returnFangyuanView.getId().intValue() ==fangyuanView.getId().intValue()) addFlag=false;//返回的数据中已存在此商品
                }
                if(addFlag){
                    toAddNum=toAddNum-1;
                    returnFangyuanViewList.add(fangyuanView);
                    if(toAddNum==0) break;//够数量了
                }
            }
        }else {
            returnFangyuanViewList = returnFangyuanViewList.subList(0, limit);
        }

        for(FangyuanView c:returnFangyuanViewList)
            dictionaryService.dictionaryConvert(c, request);
        page.setList(returnFangyuanViewList);
        return R.ok().put("data", page);
    }

    /**
    * 前端列表
    */
    @IgnoreAuth
    @RequestMapping("/list")
    public R list(@RequestParam Map<String, Object> params, HttpServletRequest request){
        logger.debug("list方法:,,Controller:{},,params:{}",this.getClass().getName(),JSONObject.toJSONString(params));

        CommonUtil.checkMap(params);
        PageUtils page = fangyuanService.queryPage(params);

        //字典表数据转换
        List<FangyuanView> list =(List<FangyuanView>)page.getList();
        for(FangyuanView c:list)
            dictionaryService.dictionaryConvert(c, request); //修改对应字典表字段

        return R.ok().put("data", page);
    }

    /**
    * 前端详情
    */
    @RequestMapping("/detail/{id}")
    public R detail(@PathVariable("id") Long id, HttpServletRequest request){
        logger.debug("detail方法:,,Controller:{},,id:{}",this.getClass().getName(),id);
        FangyuanEntity fangyuan = fangyuanService.selectById(id);
            if(fangyuan !=null){


                //entity转view
                FangyuanView view = new FangyuanView();
                BeanUtils.copyProperties( fangyuan , view );//把实体数据重构到view中

                //修改对应字典表字段
                dictionaryService.dictionaryConvert(view, request);
                return R.ok().put("data", view);
            }else {
                return R.error(511,"查不到数据");
            }
    }


    /**
    * 前端保存
    */
    @RequestMapping("/add")
    public R add(@RequestBody FangyuanEntity fangyuan, HttpServletRequest request){
        logger.debug("add方法:,,Controller:{},,fangyuan:{}",this.getClass().getName(),fangyuan.toString());
        Wrapper<FangyuanEntity> queryWrapper = new EntityWrapper<FangyuanEntity>()
            .eq("fangyuan_name", fangyuan.getFangyuanName())
            .eq("fangyuan_uuid_number", fangyuan.getFangyuanUuidNumber())
            .eq("fangyuan_types", fangyuan.getFangyuanTypes())
            .eq("fangyuan_kucun_number", fangyuan.getFangyuanKucunNumber())
            .eq("fangyuan_delete", fangyuan.getFangyuanDelete())
//            .notIn("fangyuan_types", new Integer[]{102})
            ;
        logger.info("sql语句:"+queryWrapper.getSqlSegment());
        FangyuanEntity fangyuanEntity = fangyuanService.selectOne(queryWrapper);
        if(fangyuanEntity==null){
            fangyuan.setFangyuanDelete(1);
            fangyuan.setCreateTime(new Date());
        fangyuanService.insert(fangyuan);

            return R.ok();
        }else {
            return R.error(511,"表中有相同数据");
        }
    }

}

