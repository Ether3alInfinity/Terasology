/*
 * Copyright 2014 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.rendering.nui.mainMenu;

import org.terasology.config.Config;
import org.terasology.config.ServerInfo;
import org.terasology.engine.GameEngine;
import org.terasology.engine.TerasologyConstants;
import org.terasology.engine.modes.StateLoading;
import org.terasology.entitySystem.systems.In;
import org.terasology.network.JoinStatus;
import org.terasology.network.NetworkSystem;
import org.terasology.rendering.nui.NUIManager;
import org.terasology.rendering.nui.UIScreenLayer;
import org.terasology.rendering.nui.UIScreenLayerUtil;
import org.terasology.rendering.nui.baseWidgets.ButtonEventListener;
import org.terasology.rendering.nui.baseWidgets.ListEventListener;
import org.terasology.rendering.nui.baseWidgets.UIButton;
import org.terasology.rendering.nui.baseWidgets.UILabel;
import org.terasology.rendering.nui.baseWidgets.UIList;
import org.terasology.rendering.nui.databinding.BindHelper;
import org.terasology.rendering.nui.databinding.ListSelectionBinding;
import org.terasology.rendering.nui.itemRendering.StringTextRenderer;

/**
 * @author Immortius
 */
public class JoinGameScreen extends UIScreenLayer {

    @In
    private NUIManager nuiManager;

    @In
    private Config config;

    @In
    private NetworkSystem networkSystem;

    @In
    private GameEngine engine;

    private UIList<ServerInfo> serverList;

    @Override
    public void initialise() {
        serverList = find("serverList", UIList.class);
        if (serverList != null) {
            serverList.bindList(BindHelper.bindBeanListProperty("servers", config.getNetwork(), ServerInfo.class));
            serverList.setItemRenderer(new StringTextRenderer<ServerInfo>() {
                @Override
                public String getString(ServerInfo value) {
                    return value.getName();
                }
            });
            serverList.subscribe(new ListEventListener<ServerInfo>() {
                @Override
                public void onItemActivated(ServerInfo item) {
                    join(item.getAddress());
                }
            });

            UILabel name = find("name", UILabel.class);
            name.bindText(BindHelper.bindBoundBeanProperty("name", new ListSelectionBinding<ServerInfo>(serverList), ServerInfo.class, String.class));

            UILabel address = find("address", UILabel.class);
            address.bindText(BindHelper.bindBoundBeanProperty("address", new ListSelectionBinding<ServerInfo>(serverList), ServerInfo.class, String.class));

            UIScreenLayerUtil.trySubscribe(this, "add", new ButtonEventListener() {
                @Override
                public void onButtonActivated(UIButton button) {
                    nuiManager.pushScreen("engine:addServerPopup");
                }
            });
            UIScreenLayerUtil.trySubscribe(this, "remove", new ButtonEventListener() {
                @Override
                public void onButtonActivated(UIButton button) {
                    if (serverList.getSelection() != null) {
                        config.getNetwork().remove(serverList.getSelection());
                        serverList.setSelection(null);
                    }
                }
            });
            UIScreenLayerUtil.trySubscribe(this, "join", new ButtonEventListener() {
                @Override
                public void onButtonActivated(UIButton button) {
                    config.save();
                    if (serverList.getSelection() != null) {
                        join(serverList.getSelection().getAddress());
                    }
                }
            });
        }
        UIScreenLayerUtil.trySubscribe(this, "joinDirect", new ButtonEventListener() {
            @Override
            public void onButtonActivated(UIButton button) {
                config.save();
                nuiManager.pushScreen("engine:joinServerPopup");
            }
        });


        UIScreenLayerUtil.trySubscribe(this, "close", new ButtonEventListener() {
            @Override
            public void onButtonActivated(UIButton button) {
                config.save();
                nuiManager.popScreen();
            }
        });
    }

    private void join(String address) {
        JoinStatus joinStatus = networkSystem.join(address, TerasologyConstants.DEFAULT_PORT);
        if (joinStatus.getStatus() != JoinStatus.Status.FAILED) {
            engine.changeState(new StateLoading(joinStatus));
        } else {
            nuiManager.pushScreen("engine:errorMessagePopup", ErrorMessagePopup.class)
                    .setError("Failed to Join", "Could not connect to server - " + joinStatus.getErrorMessage());
        }
    }
}
