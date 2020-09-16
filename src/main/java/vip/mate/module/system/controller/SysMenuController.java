package vip.mate.module.system.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;
import vip.mate.core.common.api.Result;
import vip.mate.core.common.constant.MateConstant;
import vip.mate.core.common.entity.LoginUser;
import vip.mate.core.util.TreeUtil;
import vip.mate.core.web.annotation.EnableUser;
import vip.mate.core.web.controller.BaseController;
import vip.mate.core.web.tree.ForestNodeMerger;
import vip.mate.core.web.util.CollectionUtil;
import vip.mate.core.file.util.ExcelUtil;
import vip.mate.module.system.dto.SysMenuDTO;
import vip.mate.module.system.entity.SysMenu;
import vip.mate.module.system.poi.SysMenuPOI;
import vip.mate.module.system.service.ISysMenuService;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * 菜单权限表 前端控制器
 * </p>
 *
 * @author xuzf
 * @since 2020-06-18
 */
@Slf4j
@RestController
@AllArgsConstructor
@RequestMapping("/mate-system/menu")
@Api(tags = "菜单管理")
public class SysMenuController extends BaseController {

    private final ISysMenuService sysMenuService;

    /**
     * 菜单树
     *
     * @return Result
     */
    @GetMapping("/tree")
    @ApiOperation(value = "菜单树", notes = "根据roleId查询菜单树")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "roleId", required = false, value = "可不填，根据token获取", paramType = "form"),
    })
    public Result<?> tree(@ApiIgnore @EnableUser LoginUser user) {
        return Result.data(sysMenuService.routes(user.getRoleId()));
    }

    /**
     * 菜单列表
     *
     * @param search 搜索关键词
     * @return Result
     */
    @GetMapping("/list")
    @ApiOperation(value = "菜单列表", notes = "菜单列表，根据关键词查询")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "keyword", required = true, value = "模糊查询关键词", paramType = "form"),
            @ApiImplicitParam(name = "startDate", required = true, value = "创建开始日期", paramType = "form"),
            @ApiImplicitParam(name = "endDate", required = true, value = "创建结束日期", paramType = "form"),
    })
    public Result<?> list(@ApiIgnore @RequestParam Map<String, Object> search) {
        return Result.data(TreeUtil.list2Tree(sysMenuService.searchList(search), MateConstant.TREE_ROOT));
    }

    /**
     * 菜单分级列表，用于前端下拉框使用
     *
     * @return Result
     */
    @GetMapping("/grade")
    @ApiOperation(value = "菜单分级列表", notes = "菜单分级列表")
    public Result<?> grade() {
        LambdaQueryWrapper<SysMenu> lsm = Wrappers.<SysMenu>query().lambda().orderByAsc(SysMenu::getSort);
        List<SysMenu> sysMenus = sysMenuService.list(lsm);
        List<SysMenuDTO> sysMenuDTOS = sysMenus.stream().map(sysMenu -> {
            SysMenuDTO sysMenuDTO = new SysMenuDTO();
            sysMenuDTO.setId(sysMenu.getId());
            sysMenuDTO.setLabel(sysMenu.getName());
            sysMenuDTO.setParentId(sysMenu.getParentId());
            return sysMenuDTO;
        }).collect(Collectors.toList());
        return Result.data(ForestNodeMerger.merge(sysMenuDTOS));
    }

    /**
     * 菜单设置
     *
     * @param sysMenu 菜单
     * @return Result
     */
    @PostMapping("/set")
    @ApiOperation(value = "菜单设置", notes = "菜单设置,支持新增或修改")
    public Result<?> set(@Valid @RequestBody SysMenu sysMenu) {
        return Result.condition(sysMenuService.saveAll(sysMenu));
    }

    /**
     * 查询菜单信息
     *
     * @param id id
     * @return Result
     */
    @GetMapping("/get")
    @ApiOperation(value = "菜单信息", notes = "根据ID查询")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", required = true, value = "菜单ID", paramType = "form"),
    })
    public Result<?> get(@RequestParam String id) {
        LambdaQueryWrapper<SysMenu> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(SysMenu::getId, id);
        return Result.data(sysMenuService.getOne(queryWrapper));
    }

    /**
     * 批量删除查询
     *
     * @param ids 多个Id，用,号分隔
     * @return Result
     */
    @PostMapping("/del")
    @ApiOperation(value = "菜单删除", notes = "菜单删除")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", required = true, value = "多个用,号隔开", paramType = "form")
    })
    public Result<?> del(@RequestParam String ids) {
        return Result.condition(sysMenuService.removeByIds(CollectionUtil.stringToCollection(ids)));
    }

    /**
     * 菜单状态
     *
     * @param ids    多个Id，用,号分隔
     * @param status 状态：启用、禁用
     * @return Result
     */
    @PostMapping("/set-status")
    @ApiOperation(value = "菜单状态", notes = "状态包括：启用、禁用")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "ids", required = true, value = "多个id用,号隔开", paramType = "form"),
            @ApiImplicitParam(name = "status", required = true, value = "状态", paramType = "form")
    })
    public Result<?> setStatus(@RequestParam String ids, @RequestParam String status) {
        return Result.condition(sysMenuService.status(ids, status));
    }

    /**
     * 菜单导出
     */
    @PostMapping("/export")
    @ApiOperation(value = "菜单导出", notes = "菜单导出")
    public void export(@ApiIgnore HttpServletResponse response) {
        List<SysMenuPOI> sysMenuPOIS = sysMenuService.export();
        //使用工具类导出excel
        ExcelUtil.exportExcel(sysMenuPOIS, null, "菜单", SysMenuPOI.class, "menu", response);
    }
}

